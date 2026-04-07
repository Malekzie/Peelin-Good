package com.sait.peelin.service;

import com.sait.peelin.model.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final double TAX_RATE = 0.13;
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a z", Locale.CANADA);
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.CANADA);

    // Optional — null when MAIL_USERNAME is not set and Spring cannot create the bean.
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String fromAddress;

    /**
     * Sends an HTML order confirmation email to the customer who placed the order.
     * Silently skips sending if SMTP is not configured.
     *
     * @param order the fulfilled order
     * @param items all line items belonging to the order
     */
    public void sendOrderConfirmation(Order order, List<OrderItem> items) {
        if (mailSender == null) {
            log.debug("SMTP not configured — skipping order confirmation email for {}", order.getOrderNumber());
            return;
        }

        Customer customer = order.getCustomer();
        if (customer == null) {
            log.warn("Order {} has no customer — skipping confirmation email", order.getOrderNumber());
            return;
        }

        String toEmail = customer.getCustomerEmail();
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Customer on order {} has no email address — skipping confirmation email", order.getOrderNumber());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress.isBlank() ? toEmail : fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Order Confirmed \u2014 " + order.getOrderNumber());
            helper.setText(buildHtml(order, items, customer), true);
            mailSender.send(message);
            log.info("Confirmation email sent to {} for order {}", toEmail, order.getOrderNumber());
        } catch (MessagingException e) {
            log.error("Failed to send confirmation email for order {}", order.getOrderNumber(), e);
        }
    }

    // -------------------------------------------------------------------------
    // HTML builder
    // -------------------------------------------------------------------------

    private String buildHtml(Order order, List<OrderItem> items, Customer customer) {
        BigDecimal subtotalAfterDiscount = order.getOrderTotal() != null
                ? order.getOrderTotal() : BigDecimal.ZERO;
        BigDecimal discount = order.getOrderDiscount() != null
                ? order.getOrderDiscount() : BigDecimal.ZERO;
        BigDecimal listSubtotal = subtotalAfterDiscount.add(discount);
        BigDecimal tax = subtotalAfterDiscount
                .multiply(BigDecimal.valueOf(TAX_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotalAfterDiscount.add(tax);

        String firstName = customer.getCustomerFirstName() != null
                ? customer.getCustomerFirstName() : "Valued Customer";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>")
            .append("<meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            .append("</head>")
            .append("<body style='margin:0;padding:0;background:#f5f0eb;font-family:Arial,Helvetica,sans-serif;'>")
            .append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f5f0eb;padding:32px 0;'>")
            .append("<tr><td align='center'>")
            .append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;'>");

        // Header
        html.append("<tr><td style='background:#5c3d2e;padding:32px 40px;border-radius:8px 8px 0 0;text-align:center;'>")
            .append("<h1 style='margin:0;color:#fff;font-size:26px;letter-spacing:1px;'>Peelin\u2019 Good</h1>")
            .append("<p style='margin:8px 0 0;color:#e0cfc4;font-size:14px;'>Your order is confirmed!</p>")
            .append("</td></tr>");

        // Body
        html.append("<tr><td style='background:#fff;padding:40px;'>")

            // Greeting
            .append("<p style='margin:0 0 24px;font-size:16px;color:#333;'>")
            .append("Hi ").append(esc(firstName)).append(",</p>")
            .append("<p style='margin:0 0 32px;font-size:15px;color:#555;line-height:1.6;'>")
            .append("Thank you for your order! Your payment has been received and your order is being prepared.")
            .append("</p>");

        // Order meta
        html.append("<table width='100%' cellpadding='8' cellspacing='0' ")
            .append("style='background:#faf7f4;border:1px solid #e8e0d8;border-radius:6px;margin-bottom:32px;'>")
            .append(metaRow("Order Number", "<strong>" + esc(order.getOrderNumber()) + "</strong>"))
            .append(metaRow("Placed", formatDt(order.getOrderPlacedDatetime())))
            .append(metaRow("Scheduled", formatDt(order.getOrderScheduledDatetime())))
            .append(metaRow("Method", capitalize(order.getOrderMethod() != null ? order.getOrderMethod().name() : "")))
            .append(metaRow("Location", buildLocationHtml(order)));

        if (order.getOrderComment() != null && !order.getOrderComment().isBlank()) {
            html.append(metaRow("Note", esc(order.getOrderComment())));
        }
        html.append("</table>");

        // Items header
        html.append("<h3 style='margin:0 0 12px;color:#5c3d2e;font-size:16px;'>Your Items</h3>")
            .append("<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:24px;'>")
            .append("<tr style='border-bottom:2px solid #e8e0d8;'>")
            .append("<td style='padding:6px 0;color:#888;font-size:13px;'>Item</td>")
            .append("<td style='padding:6px 0;color:#888;font-size:13px;text-align:center;'>Qty</td>")
            .append("<td style='padding:6px 0;color:#888;font-size:13px;text-align:right;'>Unit</td>")
            .append("<td style='padding:6px 0;color:#888;font-size:13px;text-align:right;'>Total</td>")
            .append("</tr>");

        for (OrderItem item : items) {
            String name = item.getProduct() != null ? item.getProduct().getProductName() : "Item";
            html.append("<tr style='border-bottom:1px solid #f0ebe4;'>")
                .append("<td style='padding:10px 0;font-size:14px;color:#333;'>").append(esc(name)).append("</td>")
                .append("<td style='padding:10px 0;font-size:14px;color:#333;text-align:center;'>")
                .append(item.getOrderItemQuantity()).append("</td>")
                .append("<td style='padding:10px 0;font-size:14px;color:#333;text-align:right;'>")
                .append(fmt(item.getOrderItemUnitPriceAtTime())).append("</td>")
                .append("<td style='padding:10px 0;font-size:14px;color:#333;text-align:right;'>")
                .append(fmt(item.getOrderItemLineTotal())).append("</td>")
                .append("</tr>");
        }
        html.append("</table>");

        // Totals
        html.append("<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:32px;'>")
            .append("<tr style='border-top:1px solid #e8e0d8;'><td colspan='2' style='padding-top:8px;'></td></tr>");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            html.append(totalsRow("List Subtotal", fmt(listSubtotal), "#555", false))
                .append(totalsRow("Discount", "\u2212" + fmt(discount), "#2e7d32", false));
        }

        html.append(totalsRow("Subtotal", fmt(subtotalAfterDiscount), "#333", false))
            .append(totalsRow("Tax (13%)", fmt(tax), "#555", false))
            .append("<tr><td colspan='2' style='padding:4px 0;'>"
                + "<hr style='border:none;border-top:2px solid #5c3d2e;margin:4px 0;'></td></tr>")
            .append(totalsRow("Total", fmt(grandTotal), "#5c3d2e", true))
            .append("</table>");

        // Footer note
        html.append("<p style='margin:0;font-size:13px;color:#999;line-height:1.6;'>")
            .append("If you have any questions about your order, reply to this email or contact us at ")
            .append("<a href='mailto:").append(esc(order.getBakery() != null ? order.getBakery().getBakeryEmail() : ""))
            .append("' style='color:#5c3d2e;'>")
            .append(esc(order.getBakery() != null ? order.getBakery().getBakeryEmail() : "the bakery"))
            .append("</a>.</p>");

        // Close body card
        html.append("</td></tr>");

        // Footer bar
        html.append("<tr><td style='background:#e8e0d8;padding:16px 40px;border-radius:0 0 8px 8px;text-align:center;'>")
            .append("<p style='margin:0;font-size:12px;color:#888;'>")
            .append("\u00a9 Peelin\u2019 Good \u2014 This email was sent to ")
            .append(esc(customer.getCustomerEmail()))
            .append(" because you placed an order.")
            .append("</p></td></tr>");

        html.append("</table></td></tr></table></body></html>");
        return html.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildLocationHtml(Order order) {
        boolean isDelivery = order.getOrderMethod() != null
                && order.getOrderMethod().name().equalsIgnoreCase("delivery");

        if (isDelivery) {
            Address addr = order.getAddress();
            if (addr == null) return "Delivery address not recorded";
            return "Delivery to: " + formatAddress(addr);
        } else {
            Bakery bakery = order.getBakery();
            if (bakery == null) return "Pickup location not recorded";
            String name = esc(bakery.getBakeryName());
            String addr = bakery.getAddress() != null ? formatAddress(bakery.getAddress()) : "";
            return "Pickup at " + name + (addr.isBlank() ? "" : "<br>" + addr);
        }
    }

    private String formatAddress(Address a) {
        StringBuilder sb = new StringBuilder(esc(a.getAddressLine1()));
        if (a.getAddressLine2() != null && !a.getAddressLine2().isBlank()) {
            sb.append(", ").append(esc(a.getAddressLine2()));
        }
        sb.append("<br>").append(esc(a.getAddressCity()))
          .append(", ").append(esc(a.getAddressProvince()))
          .append(" ").append(esc(a.getAddressPostalCode()));
        return sb.toString();
    }

    private String metaRow(String label, String value) {
        return "<tr>"
                + "<td style='width:140px;font-size:13px;color:#888;white-space:nowrap;vertical-align:top;'>"
                + esc(label) + "</td>"
                + "<td style='font-size:14px;color:#333;'>" + value + "</td>"
                + "</tr>";
    }

    private String totalsRow(String label, String value, String color, boolean bold) {
        String weight = bold ? "bold" : "normal";
        String size   = bold ? "16px" : "14px";
        return "<tr>"
                + "<td style='padding:5px 0;font-size:" + size + ";color:" + color + ";font-weight:" + weight + ";'>"
                + esc(label) + "</td>"
                + "<td style='padding:5px 0;font-size:" + size + ";color:" + color + ";font-weight:" + weight
                + ";text-align:right;'>" + value + "</td>"
                + "</tr>";
    }

    private String formatDt(OffsetDateTime dt) {
        if (dt == null) return "\u2014";
        return esc(dt.format(DT_FMT));
    }

    private String fmt(BigDecimal amount) {
        if (amount == null) return CURRENCY.format(0);
        return esc(CURRENCY.format(amount.doubleValue()));
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    /** Basic HTML entity escaping to prevent XSS in dynamic content. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
