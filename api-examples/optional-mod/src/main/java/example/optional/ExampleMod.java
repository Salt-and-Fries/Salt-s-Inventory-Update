package example.optional;

/** The ordinary mod entrypoint has no Salt imports, fields, signatures or class literals. */
public final class ExampleMod {
    public static void initialize() {
        System.setProperty("salts.example.normal", "ready");
    }

    public static void main(String[] arguments) {
        initialize();
    }
}
