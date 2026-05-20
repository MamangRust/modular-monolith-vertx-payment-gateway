package io.example.common.utils;

import java.util.Map;

public class EmailTemplate {
  public static String generateHtml(Map<String, String> data) {
    String title = data.getOrDefault("Title", "");
    String message = data.getOrDefault("Message", "");
    String buttonLabel = data.getOrDefault("Button", "");
    String link = data.getOrDefault("Link", "");

    return String.format("""
        <!DOCTYPE html>
        <html>
        <head>
          <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
            .container { width: 80%%; margin: 20px auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px; }
            .header { font-size: 24px; font-weight: bold; color: #2c3e50; margin-bottom: 20px; }
            .content { margin-bottom: 30px; }
            .button { display: inline-block; padding: 12px 24px; background-color: #3498db; color: #ffffff !important; text-decoration: none; border-radius: 4px; font-weight: bold; }
            .footer { margin-top: 30px; font-size: 12px; color: #7f8c8d; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">%s</div>
            <div class="content">
              %s
            </div>
            %s
            <div class="footer">
              This is an automated message from SanEdge. Please do not reply.
            </div>
          </div>
        </body>
        </html>
        """, 
        title, 
        message, 
        (buttonLabel.isEmpty() || link.isEmpty()) ? "" : 
            String.format("<a href=\"%s\" class=\"button\">%s</a>", link, buttonLabel)
    );
  }
}
