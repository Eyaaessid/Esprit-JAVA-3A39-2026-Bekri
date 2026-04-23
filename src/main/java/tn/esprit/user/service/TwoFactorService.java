package tn.esprit.user.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.EncodeHintType;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.jboss.aerogear.security.otp.api.Base32;
import tn.esprit.user.entity.Utilisateur;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwoFactorService {
    private static final String ISSUER = "Bekri Wellbeing";
    private static final int DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateSecret() {
        return Base32.random();
    }

    public boolean verifyCode(String secret, String userCode) {
        if (secret == null || secret.isBlank() || userCode == null || !userCode.matches("\\d{6}")) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000L;
        long counter = now / PERIOD_SECONDS;
        for (long c = counter - 1; c <= counter + 1; c++) {
            String generated = generateTotp(secret, c);
            if (generated != null && MessageDigest.isEqual(generated.getBytes(StandardCharsets.UTF_8),
                    userCode.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    public String getQrCodeUri(Utilisateur user) {
        if (user == null || user.getEmail() == null || user.getTotpSecret() == null) {
            throw new IllegalArgumentException("Utilisateur ou secret TOTP invalide");
        }
        String issuerEncoded = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        String emailEncoded = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
        return "otpauth://totp/" + issuerEncoded + ":" + emailEncoded
                + "?secret=" + user.getTotpSecret()
                + "&issuer=" + issuerEncoded;
    }

    public Image generateQrCodeImage(String uri, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (WriterException e) {
            throw new RuntimeException("Impossible de générer le QR code 2FA", e);
        }
    }

    public List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            byte[] bytes = new byte[4];
            SECURE_RANDOM.nextBytes(bytes);
            codes.add(HexFormat.of().formatHex(bytes));
        }
        return codes;
    }

    public List<String> hashBackupCodes(List<String> plainCodes) {
        List<String> hashes = new ArrayList<>();
        if (plainCodes == null) {
            return hashes;
        }
        for (String code : plainCodes) {
            hashes.add(sha256Hex(code));
        }
        return hashes;
    }

    public boolean verifyBackupCode(String plainCode, List<String> storedHashes) {
        if (plainCode == null || plainCode.isBlank() || storedHashes == null || storedHashes.isEmpty()) {
            return false;
        }
        String enteredHash = sha256Hex(plainCode.trim());
        byte[] enteredBytes = enteredHash.getBytes(StandardCharsets.UTF_8);
        for (String storedHash : storedHashes) {
            if (storedHash == null) {
                continue;
            }
            byte[] storedBytes = storedHash.getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(enteredBytes, storedBytes)) {
                return true;
            }
        }
        return false;
    }

    public List<String> removeUsedBackupCode(String plainCode, List<String> storedHashes) {
        List<String> result = new ArrayList<>();
        if (storedHashes == null) {
            return result;
        }
        String enteredHash = sha256Hex(plainCode == null ? "" : plainCode.trim());
        byte[] enteredBytes = enteredHash.getBytes(StandardCharsets.UTF_8);
        boolean removed = false;
        for (String hash : storedHashes) {
            if (!removed && hash != null && MessageDigest.isEqual(enteredBytes, hash.getBytes(StandardCharsets.UTF_8))) {
                removed = true;
                continue;
            }
            result.add(hash);
        }
        return result;
    }

    private static String generateTotp(String secret, long counter) {
        try {
            byte[] key = Base32.decode(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de hachage SHA-256", e);
        }
    }
}
