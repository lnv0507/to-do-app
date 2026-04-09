package vn.com.anhemsoftware.license_app.util;

import vn.com.anhemsoftware.license_app.payload.auth.internal.GeoLocation;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Builds clean, minimal HTML email templates inspired by Canva-style
 * notifications.
 * Simple layout: logo · greeting · metadata rows · one CTA.
 */
public class EmailTemplateBuilder {

  // ─── Shared styles (intentionally minimal) ────────────────────────────────

  private static final String BASE_STYLES = """
      <style>
        *{box-sizing:border-box;margin:0;padding:0}
        body{background:#f5f5f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;color:#111111}
        .wrap{max-width:560px;margin:48px auto;background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 1px 6px rgba(0,0,0,.08)}
        .top-bar{height:4px;background:#7c3aed}
        .content{padding:40px 48px}
        .logo{font-size:18px;font-weight:700;color:#7c3aed;letter-spacing:-.3px;margin-bottom:28px}
        h1{font-size:20px;font-weight:700;color:#111111;line-height:1.35;margin-bottom:12px}
        .lead{font-size:15px;color:#444444;line-height:1.6;margin-bottom:28px}
        .meta-table{width:100%;border-collapse:collapse;margin-bottom:28px}
        .meta-table td{padding:9px 0;font-size:14px;border-bottom:1px solid #f0f0f0;vertical-align:top}
        .meta-table td:first-child{color:#888888;width:90px;padding-right:12px;white-space:nowrap}
        .meta-table td:last-child{color:#111111;font-weight:500;word-break:break-word}
        .note{font-size:14px;color:#444444;line-height:1.6;margin-bottom:28px}
        .note strong{color:#111111}
        .divider{border:none;border-top:1px solid #f0f0f0;margin:28px 0}
        .footer{padding:24px 48px;background:#fafafa;border-top:1px solid #f0f0f0;font-size:12px;color:#aaaaaa;line-height:1.6}
      </style>
      """;

  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy HH:mm:ss 'UTC'",
      Locale.ENGLISH);

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private static String now() {
    return ZonedDateTime.now(ZoneOffset.UTC).format(TIME_FMT);
  }

  private static String locationDisplay(GeoLocation geo) {
    if (geo == null)
      return "Unknown";
    if (geo.isLocal())
      return "Local Network";
    String city = (geo.city() != null && !geo.city().isBlank()) ? geo.city() : null;
    String country = (geo.country() != null && !geo.country().isBlank()) ? geo.country() : null;
    if (city != null && country != null)
      return city + ", " + country;
    if (city != null)
      return city;
    if (country != null)
      return country;
    return "Unknown";
  }

  /** Simplified UA: "Windows 10, Chrome" instead of the full UA string. */
  private static String friendlyAgent(String ua) {
    if (ua == null || ua.isBlank())
      return "Unknown device";
    String os = "Unknown OS";
    if (ua.contains("Windows NT 10"))
      os = "Windows 10";
    else if (ua.contains("Windows NT 11"))
      os = "Windows 11";
    else if (ua.contains("Windows"))
      os = "Windows";
    else if (ua.contains("Mac OS X"))
      os = "macOS";
    else if (ua.contains("Android"))
      os = "Android";
    else if (ua.contains("iPhone") || ua.contains("iPad"))
      os = "iOS";
    else if (ua.contains("Linux"))
      os = "Linux";

    String browser = "Unknown Browser";
    if (ua.contains("Edg/"))
      browser = "Edge";
    else if (ua.contains("OPR/"))
      browser = "Opera";
    else if (ua.contains("Chrome/"))
      browser = "Chrome";
    else if (ua.contains("Firefox/"))
      browser = "Firefox";
    else if (ua.contains("Safari/"))
      browser = "Safari";

    return os + ", " + browser;
  }

  private static String escape(String s) {
    if (s == null)
      return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  // ─── Page shell ───────────────────────────────────────────────────────────

  private static String page(String bodyContent) {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1"/>
          <title>Security Notification</title>
        """ + BASE_STYLES + """
        </head>
        <body>
        <div class="wrap">
          <div class="top-bar"></div>
          <div class="content">
            <div class="logo">Anhem ✦</div>
        """ + bodyContent + """
          </div>
          <div class="footer">
            This is an automated security notification from Anhem Software.<br/>
            Please do not reply to this email. Questions? Contact
            <a href="mailto:support@anhemsoftware.vn" style="color:#7c3aed">support@anhemsoftware.vn</a>
          </div>
        </div>
        </body>
        </html>
        """;
  }

  // ─── Public builders ──────────────────────────────────────────────────────

  /**
   * MEDIUM risk — new device, familiar location.
   * Access was granted; user is warned and can reset if needed.
   */
  public static String newDeviceWarning(String userAgent, String ip, GeoLocation geo, String resetLink) {
    return page("""
        <h1>New sign-in to your Anhem account</h1>
        <p class="lead">
          We detected a new or unusual sign-in to your Anhem account
          from a device we haven't seen before.
        </p>
        <table class="meta-table">
          <tr><td>Device</td><td>%s</td></tr>
          <tr><td>Location</td><td>%s</td></tr>
          <tr><td>IP Address</td><td>%s</td></tr>
          <tr><td>Time</td><td>%s</td></tr>
        </table>
        <p class="note">
          <strong>If this was you</strong>, you can ignore this email and keep working!<br/><br/>
          <strong>If this wasn't you</strong>, you should reset your password immediately
          to secure your account.
        </p>
        <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:24px 0 8px">
          <tr>
            <td align="left">
              <a href="%s"
                 target="_blank"
                 style="display:inline-block;
                        background-color:#7c3aed;
                        color:#ffffff;
                        font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;
                        font-size:15px;
                        font-weight:700;
                        text-decoration:none;
                        padding:14px 32px;
                        border-radius:8px;
                        mso-padding-alt:14px 32px;
                        letter-spacing:.2px">
                &#x1F512;&nbsp; Reset My Password
              </a>
            </td>
          </tr>
        </table>
        <p style="font-size:12px;color:#aaaaaa;margin:0">This link expires in 2 hours.</p>
        """.formatted(
        escape(friendlyAgent(userAgent)),
        escape(locationDisplay(geo)),
        escape(ip),
        now(),
        escape(resetLink)));
  }

  /**
   * HIGH risk — new device, unknown location. Sign-in was blocked; OTP required.
   */
  public static String highRiskOtp(String userAgent, String ip, GeoLocation geo,
      String otp, long expiryMinutes) {
    return page(
        """
            <h1>Sign-in attempt blocked — verify it's you</h1>
            <p class="lead">
              Someone tried to sign in to your Anhem account from an unrecognized
              device and location. We've blocked this attempt to keep your account safe.
            </p>
            <table class="meta-table">
              <tr><td>Device</td><td>%s</td></tr>
              <tr><td>Location</td><td>%s</td></tr>
              <tr><td>IP Address</td><td>%s</td></tr>
              <tr><td>Time</td><td>%s</td></tr>
            </table>
            <p class="note">
              <strong>If this was you</strong>, enter the one-time code below in the app to verify your identity.
            </p>
            <div style="background:#f5f0ff;border-radius:8px;padding:24px;text-align:center;margin-bottom:28px">
              <p style="font-size:12px;color:#888888;letter-spacing:1px;text-transform:uppercase;margin-bottom:8px">Your verification code</p>
              <p style="font-size:40px;font-weight:800;letter-spacing:10px;color:#7c3aed;font-family:'Courier New',monospace">%s</p>
              <p style="font-size:12px;color:#aaaaaa;margin-top:8px">Expires in %d minutes &bull; Do not share this code</p>
            </div>
            <p class="note">
              <strong>If this wasn't you</strong>, you can safely ignore this email.
              Your account is still protected and the sign-in was blocked.
            </p>
            """
            .formatted(
                escape(friendlyAgent(userAgent)),
                escape(locationDisplay(geo)),
                escape(ip),
                now(),
                otp,
                expiryMinutes));
  }

  /**
   * Password changed / reset success notification.
   */
  public static String passwordChangedNotification() {
    return page("""
        <h1>Your password was changed</h1>
        <p class="lead">
          Your Anhem account password has been updated successfully.
          All other active sessions have been signed out.
        </p>
        <p class="note">
          <strong>If this was you</strong>, no further action is needed.<br/><br/>
          <strong>If this wasn't you</strong>, contact our support team immediately at
          <a href="mailto:support@anhemsoftware.vn" style="color:#7c3aed">support@anhemsoftware.vn</a>
          so we can help secure your account.
        </p>
        <hr class="divider"/>
        <p style="font-size:12px;color:#aaaaaa">Changed on: %s</p>
        """.formatted(now()));
  }
}
