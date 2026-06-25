package eventsystem.util;

public class PasswordUtilTest {

    public static void main(String[] args) {

        String rawPassword = "123456";

        String hashedPassword = PasswordUtil.hashPassword(rawPassword);

        System.out.println("Generated Hash:");
        System.out.println(hashedPassword);

        boolean correctPassword = PasswordUtil.verifyPassword("123456", hashedPassword);
        boolean wrongPassword = PasswordUtil.verifyPassword("wrong", hashedPassword);

        boolean oldPlainPasswordSupport = PasswordUtil.verifyPassword("123456", "123456");

        System.out.println("Correct password result: " + correctPassword);
        System.out.println("Wrong password result: " + wrongPassword);
        System.out.println("Old plain password support result: " + oldPlainPasswordSupport);

        if (correctPassword && !wrongPassword && oldPlainPasswordSupport) {
            System.out.println("PasswordUtil test PASSED.");
        } else {
            System.out.println("PasswordUtil test FAILED.");
        }
    }
}