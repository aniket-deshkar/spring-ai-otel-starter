package io.github.aniketdeshkar.aiotel;

public record CapturedContent(String prompt, String toolArguments) {
  public static CapturedContent none() {
    return new CapturedContent(null, null);
  }

  public static CapturedContent prompt(String prompt) {
    return new CapturedContent(prompt, null);
  }

  public static CapturedContent toolArguments(String arguments) {
    return new CapturedContent(null, arguments);
  }
}
