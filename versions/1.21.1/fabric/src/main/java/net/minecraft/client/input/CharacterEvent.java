package net.minecraft.client.input;

public record CharacterEvent(char codepoint, int modifiers) {
    public boolean isAllowedChatCharacter() {
        return this.codepoint >= ' ' && this.codepoint != 127;
    }

    public String codepointAsString() {
        return Character.toString(this.codepoint);
    }
}
