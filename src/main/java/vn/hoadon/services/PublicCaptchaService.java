package vn.hoadon.services;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PublicCaptchaService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ANSWER_LENGTH = 5;
    private static final int MAX_CHALLENGES = 5_000;
    private static final Duration LIFETIME = Duration.ofMinutes(3);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaValue> challenges = new ConcurrentHashMap<>();

    public Challenge create() {
        Instant now = Instant.now();
        removeExpired(now);
        makeRoom();

        String answer = randomAnswer();
        String token = randomToken();
        byte[] salt = randomBytes(16);
        challenges.put(token, new CaptchaValue(salt, hash(salt, answer), now.plus(LIFETIME)));

        return new Challenge(token, render(answer), LIFETIME.toSeconds());
    }

    public boolean verify(String token, String answer) {
        if (token == null || token.isBlank() || answer == null || answer.isBlank()) return false;
        CaptchaValue value = challenges.remove(token.trim());
        if (value == null || Instant.now().isAfter(value.expiresAt())) return false;
        return MessageDigest.isEqual(value.answerHash(), hash(value.salt(), answer));
    }

    private String randomAnswer() {
        StringBuilder answer = new StringBuilder(ANSWER_LENGTH);
        for (int i = 0; i < ANSWER_LENGTH; i++) {
            answer.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return answer.toString();
    }

    private String randomToken() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(24));
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    private byte[] hash(byte[] salt, String answer) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(answer.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không thể khởi tạo CAPTCHA", ex);
        }
    }

    private String render(String answer) {
        BufferedImage image = new BufferedImage(180, 58, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(246, 248, 252));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

            graphics.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 9; i++) {
                graphics.setColor(randomColor(125, 205));
                graphics.drawLine(
                        random.nextInt(image.getWidth()),
                        random.nextInt(image.getHeight()),
                        random.nextInt(image.getWidth()),
                        random.nextInt(image.getHeight())
                );
            }
            for (int i = 0; i < 90; i++) {
                graphics.setColor(randomColor(130, 220));
                int size = random.nextInt(3) + 1;
                graphics.fillOval(random.nextInt(image.getWidth()), random.nextInt(image.getHeight()), size, size);
            }

            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 32);
            for (int i = 0; i < answer.length(); i++) {
                Graphics2D letter = (Graphics2D) graphics.create();
                try {
                    letter.setFont(font);
                    letter.setColor(randomColor(20, 100));
                    int x = 17 + i * 31 + random.nextInt(5);
                    int y = 39 + random.nextInt(9) - 4;
                    AffineTransform transform = AffineTransform.getRotateInstance(
                            (random.nextDouble() - 0.5d) * 0.35d,
                            x + 12,
                            y - 12
                    );
                    letter.setTransform(transform);
                    letter.drawString(String.valueOf(answer.charAt(i)), x, y);
                } finally {
                    letter.dispose();
                }
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo ảnh CAPTCHA", ex);
        }
    }

    private Color randomColor(int minimum, int maximum) {
        int range = maximum - minimum + 1;
        return new Color(
                minimum + random.nextInt(range),
                minimum + random.nextInt(range),
                minimum + random.nextInt(range)
        );
    }

    private void removeExpired(Instant now) {
        challenges.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private void makeRoom() {
        if (challenges.size() < MAX_CHALLENGES) return;
        challenges.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().expiresAt()))
                .map(Map.Entry::getKey)
                .ifPresent(challenges::remove);
    }

    public record Challenge(String token, String image, long expiresInSeconds) {}

    private record CaptchaValue(byte[] salt, byte[] answerHash, Instant expiresAt) {}
}
