package com.sait.peelin.service;

import com.sait.peelin.model.*;
import com.sait.peelin.support.GuestContactFiller;
import jakarta.mail.MessagingException;
import org.springframework.mail.MailException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
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
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("7.00");
    private static final BigDecimal DELIVERY_FREE_THRESHOLD = new BigDecimal("50.00");
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a O", Locale.CANADA);
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.CANADA);

    // Optional — null when MAIL_USERNAME is not set and Spring cannot create the bean.
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String fromAddress;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.email.logo-path:../frontend/static/images/Peelin' Good.png}")
    private String logoPath;

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

        // Resolve recipient: registered customer or guest
        String toEmail;
        String firstName;
        Customer customer = order.getCustomer();
        if (customer != null && customer.getCustomerEmail() != null
                && !GuestContactFiller.isSyntheticGuestEmail(customer.getCustomerEmail())) {
            toEmail = customer.getCustomerEmail();
            firstName = customer.getCustomerFirstName() != null ? customer.getCustomerFirstName() : "Valued Customer";
        } else if (order.getGuestEmail() != null && !order.getGuestEmail().isBlank()) {
            toEmail = order.getGuestEmail();
            firstName = order.getGuestName() != null ? order.getGuestName().split(" ")[0] : "Valued Customer";
        } else {
            log.warn("Order {} has no deliverable email address — skipping confirmation email", order.getOrderNumber());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress.isBlank() ? toEmail : fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Order Confirmed \u2014 " + order.getOrderNumber());
            helper.setText(buildHtml(order, items, firstName, toEmail), true);
            FileSystemResource logo = new FileSystemResource(logoPath);
            if (logo.exists()) {
                helper.addInline("logo", logo);
            }
            mailSender.send(message);
            log.info("Confirmation email sent to {} for order {}", toEmail, order.getOrderNumber());
        } catch (MessagingException | MailException e) {
            log.error("Failed to send confirmation email for order {}", order.getOrderNumber(), e);
        }
    }

    // -------------------------------------------------------------------------
    // HTML builder
    // -------------------------------------------------------------------------

    private String buildHtml(Order order, List<OrderItem> items, String firstName, String toEmail) {
        BigDecimal subtotalAfterDiscount = order.getOrderTotal() != null
                ? order.getOrderTotal() : BigDecimal.ZERO;
        BigDecimal discount = order.getOrderDiscount() != null
                ? order.getOrderDiscount() : BigDecimal.ZERO;
        BigDecimal listSubtotal = subtotalAfterDiscount.add(discount);
        BigDecimal tax = order.getOrderTaxAmount() != null
                ? order.getOrderTaxAmount()
                : subtotalAfterDiscount.multiply(OrderService.TAX_RATE_PERCENT)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (OrderMethod.delivery.equals(order.getOrderMethod())
                && subtotalAfterDiscount.compareTo(DELIVERY_FREE_THRESHOLD) < 0) {
            deliveryFee = DELIVERY_FEE;
        }
        BigDecimal grandTotal = subtotalAfterDiscount.add(tax).add(deliveryFee);

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
            .append("<img src='cid:logo' alt=\"Peelin\u2019 Good\" style='max-height:80px;max-width:240px;display:block;margin:0 auto 12px;' />")
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
            .append(metaRow("Est. Ready By", formatDt(resolveEstimatedReadyTime(order))))
            .append(metaRow("Method", capitalize(order.getOrderMethod() != null ? order.getOrderMethod().name() : "")))
            .append(metaRow("Location", buildLocationHtml(order)));

        if (order.getOrderComment() != null && !order.getOrderComment().isBlank()) {
            html.append(metaRow("Note", esc(order.getOrderComment())));
        }
        html.append("</table>");

        // Items
        html.append("<h3 style='margin:0 0 12px;color:#5c3d2e;font-size:16px;'>Your Items</h3>")
            .append("<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:24px;border-collapse:collapse;'>");

        for (OrderItem item : items) {
            String name = item.getProduct() != null ? item.getProduct().getProductName() : "Item";
            String imgUrl = item.getProduct() != null ? item.getProduct().getProductImageUrl() : null;
            html.append("<tr style='border-bottom:1px solid #f0ebe4;'>")
                // Image cell
                .append("<td style='padding:8px 10px 8px 0;width:56px;vertical-align:middle;'>");
            if (imgUrl != null && !imgUrl.isBlank()) {
                html.append("<img src='").append(esc(imgUrl)).append("' alt='").append(esc(name)).append("' ")
                    .append("width='48' height='48' style='border-radius:6px;display:block;object-fit:cover;' />");
            } else {
                html.append("<div style='width:48px;height:48px;border-radius:6px;background:#f5efe6;display:flex;align-items:center;justify-content:center;'></div>");
            }
            html.append("</td>")
                // Name + qty
                .append("<td style='padding:8px 8px 8px 0;font-size:14px;color:#333;vertical-align:middle;'>")
                .append("<span style='font-weight:600;'>").append(esc(name)).append("</span>")
                .append("<br><span style='font-size:12px;color:#888;'>Qty ").append(item.getOrderItemQuantity()).append("</span>")
                .append("</td>")
                // Unit price
                .append("<td style='padding:8px 8px 8px 0;font-size:13px;color:#888;text-align:right;vertical-align:middle;'>")
                .append(fmt(item.getOrderItemUnitPriceAtTime())).append(" ea")
                .append("</td>")
                // Line total
                .append("<td style='padding:8px 0;font-size:14px;color:#333;font-weight:600;text-align:right;vertical-align:middle;'>")
                .append(fmt(item.getOrderItemLineTotal()))
                .append("</td>")
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

        html.append(totalsRow("Subtotal", fmt(subtotalAfterDiscount), "#333", false));
        if (deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
            html.append(totalsRow("Delivery fee", fmt(deliveryFee), "#555", false));
        } else if (OrderMethod.delivery.equals(order.getOrderMethod())) {
            html.append(totalsRow("Delivery fee", "Free", "#2e7d32", false));
        }
        html.append(totalsRow("Tax (5%)", fmt(tax), "#555", false))
            .append("<tr><td colspan='2' style='padding:4px 0;'>"
                + "<hr style='border:none;border-top:2px solid #5c3d2e;margin:4px 0;'></td></tr>")
            .append(totalsRow("Total", fmt(grandTotal), "#5c3d2e", true))
            .append("</table>");

        // Tracking button
        String trackingUrl = frontendUrl + "/orders/" + esc(order.getOrderNumber());
        html.append("<div style='text-align:center;margin-bottom:32px;'>")
            .append("<a href='").append(trackingUrl).append("' ")
            .append("style='display:inline-block;background:#5c3d2e;color:#fff;text-decoration:none;")
            .append("font-size:14px;font-weight:bold;padding:12px 32px;border-radius:6px;'>")
            .append("Track Your Order &rarr;")
            .append("</a></div>");

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
            .append(esc(toEmail))
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

    private OffsetDateTime resolveEstimatedReadyTime(Order order) {
        if (order.getOrderScheduledDatetime() != null) {
            return order.getOrderScheduledDatetime();
        }
        // ASAP order: placed time + 2 hours, rounded up to the nearest half hour
        OffsetDateTime plusTwo = order.getOrderPlacedDatetime().plusHours(2)
                .truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        int minute = plusTwo.getMinute();
        if (minute == 0) {
            return plusTwo;
        } else if (minute <= 30) {
            return plusTwo.withMinute(30).withSecond(0).withNano(0);
        } else {
            return plusTwo.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1);
        }
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
