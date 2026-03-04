# Bakery Ecommerce - Database Design

## Problem Statement

An e-commerce bakery business sells baked goods through an online ordering platform and operates one or more bakery locations that prepare and fulfill customer orders. Each Bakery has a physical Address and produces baked goods that are offered for sale online. Customers can browse Product offerings, view product details such as name, description, and base price, and filter products using Tag classifications (for example, breads, cakes, seasonal items, or dietary attributes).

Customers may also define CustomerPreference records to personalize their experience. Each CustomerPreference links a Customer to a Tag and indicates whether the customer likes, dislikes, avoids, or is allergic to that tag, with an optional preference strength. These preferences are used to filter products, display warnings for avoided or allergy-related items, and improve personalized product recommendations.

Each Product is prepared in production Batches at a specific Bakery. The system must track batch-level information including production date, expiry date, quantity produced, and the Employee responsible for preparing the batch. Product availability depends on batch stock and may vary by bakery location. To support accurate cost and stock tracking, the system records how Inventory items (such as ingredients and packaging) supplied by a Supplier are consumed in each production batch.

Customers place Orders for either pickup or delivery from a selected bakery location. Each Order records the Customer, Bakery, fulfillment method, order lifecycle timestamps (including when the order was placed, scheduled, and completed), order status, and pricing details such as total amount and discounts applied. Delivery orders reference an Address, while pickup orders do not require one. Each order may contain multiple OrderItem entries, which specify the Product, quantity, unit price at the time of purchase, and the Batch used to fulfill the item.

The system supports user authentication and role management through a shared User account model. A User may be associated with a Customer profile, an Employee profile, or both. Customers can register, log in, manage profile details, view order history, and manage their CustomerPreference settings. Employees are associated with a bakery and may prepare production batches or moderate customer content such as reviews.

The system must support payment processing for orders and store Payment details including amount, method, transaction reference, payment status, and payment date. Refunds or failed payments must also be tracked where applicable.

The business maintains Inventory records for all supplies and materials used in production. Inventory levels are tracked per Bakery, linked to Suppliers, and adjusted based on batch production and usage. This ensures accurate stock management and accountability for ingredient consumption.

A customer loyalty program is supported through a Reward system, where customers earn points based on completed orders and may redeem points for discounts on future purchases. Customers can submit reviews and ratings for products they have purchased. Reviews include rating values, comments, submission dates, and approval status, and may be moderated by an Employee before being displayed. Review, rating, and CustomerPreference data may also be used to enhance product visibility and recommendations within the platform.

The customer loyalty program also includes a tiered rewards structure using RewardTier levels such as Bronze, Silver, and Gold. Each Customer is assigned a reward tier based on their accumulated reward points, and each tier may provide different discount rates or benefits. Reward tiers are stored separately to allow tier rules and thresholds to be adjusted without modifying customer records.

---

## Identified Classes and Attributes

### Bakery

| Column      | Type               | Constraint   |
| ----------- | ------------------ | ------------ |
| bakeryId    | INT, IDENTITY(1,1) | PK, NOT NULL |
| addressId   | INT                | FK, NOT NULL |
| bakeryName  | NVARCHAR(100)      | NOT NULL     |
| bakeryPhone | NVARCHAR(20)       | NOT NULL     |
| bakeryEmail | NVARCHAR(254)      | NOT NULL     |

### BakeryHours

| Column        | Type               | Constraint   |
| ------------- | ------------------ | ------------ |
| bakeryHoursId | INT, IDENTITY(1,1) | PK, NOT NULL |
| bakeryId      | INT                | FK, NOT NULL |
| dayOfWeek     | TINYINT            | NOT NULL     |
| openTime      | TIME               | NULL         |
| closeTime     | TIME               | NULL         |
| isClosed      | BIT                | NOT NULL     |

### Product

| Column             | Type               | Constraint   |
| ------------------ | ------------------ | ------------ |
| productId          | INT, IDENTITY(1,1) | PK, NOT NULL |
| productName        | NVARCHAR(120)      | NOT NULL     |
| productDescription | NVARCHAR(1000)     | NULL         |
| productBasePrice   | DECIMAL(10,2)      | NOT NULL     |

### Tag

| Column  | Type               | Constraint   |
| ------- | ------------------ | ------------ |
| tagId   | INT, IDENTITY(1,1) | PK, NOT NULL |
| tagName | NVARCHAR(50)       | NOT NULL     |

### ProductTag

| Column    | Type | Constraint       |
| --------- | ---- | ---------------- |
| productId | INT  | PK, FK, NOT NULL |
| tagId     | INT  | PK, FK, NOT NULL |

### Batch

| Column                | Type               | Constraint   |
| --------------------- | ------------------ | ------------ |
| batchId               | INT, IDENTITY(1,1) | PK, NOT NULL |
| bakeryId              | INT                | FK, NOT NULL |
| productId             | INT                | FK, NOT NULL |
| employeeId            | INT                | FK, NOT NULL |
| batchProductionDate   | DATE               | NOT NULL     |
| batchExpiryDate       | DATE               | NULL         |
| batchQuantityProduced | INT                | NOT NULL     |

### Employee

| Column                | Type               | Constraint   |
| --------------------- | ------------------ | ------------ |
| employeeId            | INT, IDENTITY(1,1) | PK, NOT NULL |
| userId                | INT                | FK, NOT NULL |
| addressId             | INT                | FK, NOT NULL |
| bakeryId              | INT                | FK, NOT NULL |
| employeeFirstName     | NVARCHAR(50)       | NOT NULL     |
| employeeMiddleInitial | NCHAR(2)           | NULL         |
| employeeLastName      | NVARCHAR(50)       | NOT NULL     |
| employeePosition      | NVARCHAR(40)       | NOT NULL     |
| employeePhone         | NVARCHAR(20)       | NOT NULL     |
| employeeBusinessPhone | NVARCHAR(20)       | NULL         |
| employeeWorkEmail     | NVARCHAR(254)      | NOT NULL     |

### Customer

| Column                   | Type               | Constraint   |
| ------------------------ | ------------------ | ------------ |
| customerId               | INT, IDENTITY(1,1) | PK, NOT NULL |
| userId                   | INT                | FK, NULL     |
| addressId                | INT                | FK, NOT NULL |
| rewardTierId             | INT                | FK, NOT NULL |
| customerFirstName        | NVARCHAR(50)       | NOT NULL     |
| customerMiddleInitial    | NCHAR(2)           | NULL         |
| customerLastName         | NVARCHAR(50)       | NOT NULL     |
| customerPhone            | NVARCHAR(20)       | NOT NULL     |
| customerBusinessPhone    | NVARCHAR(20)       | NULL         |
| customerRewardBalance    | INT                | NOT NULL     |
| customerTierAssignedDate | DATE               | NULL         |
| customerEmail            | NVARCHAR(254)      | NOT NULL     |

### User

| Column           | Type               | Constraint   |
| ---------------- | ------------------ | ------------ |
| userId           | INT, IDENTITY(1,1) | PK, NOT NULL |
| userUsername     | NVARCHAR(50)       | NOT NULL     |
| userEmail        | NVARCHAR(254)      | NOT NULL     |
| userPasswordHash | NVARCHAR(255)      | NOT NULL     |
| userRole         | NVARCHAR(30)       | NOT NULL     |
| userCreatedAt    | DATETIME2          | NOT NULL     |

### Address

| Column            | Type               | Constraint   |
| ----------------- | ------------------ | ------------ |
| addressId         | INT, IDENTITY(1,1) | PK, NOT NULL |
| addressLine1      | NVARCHAR(120)      | NOT NULL     |
| addressLine2      | NVARCHAR(120)      | NULL         |
| addressCity       | NVARCHAR(120)      | NOT NULL     |
| addressProvince   | NVARCHAR(80)       | NOT NULL     |
| addressPostalCode | NVARCHAR(10)       | NOT NULL     |

### Order

| Column                 | Type               | Constraint   |
| ---------------------- | ------------------ | ------------ |
| orderId                | INT, IDENTITY(1,1) | PK, NOT NULL |
| customerId             | INT                | FK, NOT NULL |
| bakeryId               | INT                | FK, NOT NULL |
| addressId              | INT                | FK, NULL     |
| orderPlacedDateTime    | DATETIME2          | NOT NULL     |
| orderScheduledDateTime | DATETIME2          | NULL         |
| orderDeliveredDateTime | DATETIME2          | NULL         |
| orderMethod            | NVARCHAR(20)       | NOT NULL     |
| orderComment           | NVARCHAR(500)      | NULL         |
| orderTotal             | DECIMAL(10,2)      | NOT NULL     |
| orderDiscount          | DECIMAL(10,2)      | NOT NULL     |
| orderStatus            | NVARCHAR(30)       | NOT NULL     |

### OrderItem

| Column                   | Type               | Constraint   |
| ------------------------ | ------------------ | ------------ |
| orderItemId              | INT, IDENTITY(1,1) | PK, NOT NULL |
| orderId                  | INT                | FK, NOT NULL |
| productId                | INT                | FK, NOT NULL |
| batchId                  | INT                | FK, NULL     |
| orderItemQuantity        | INT                | NOT NULL     |
| orderItemUnitPriceAtTime | DECIMAL(10,2)      | NOT NULL     |
| orderItemLineTotal       | DECIMAL(10,2)      | NOT NULL     |

### Payment

| Column               | Type               | Constraint   |
| -------------------- | ------------------ | ------------ |
| paymentId            | INT, IDENTITY(1,1) | PK, NOT NULL |
| orderId              | INT                | FK, NOT NULL |
| paymentAmount        | DECIMAL(10,2)      | NOT NULL     |
| paymentMethod        | NVARCHAR(30)       | NOT NULL     |
| paymentTransactionId | NVARCHAR(100)      | NULL         |
| paymentStatus        | NVARCHAR(30)       | NOT NULL     |
| paymentPaidAt        | DATETIME2          | NULL         |

### Supplier

| Column        | Type               | Constraint   |
| ------------- | ------------------ | ------------ |
| supplierId    | INT, IDENTITY(1,1) | PK, NOT NULL |
| addressId     | INT                | FK, NOT NULL |
| supplierName  | NVARCHAR(120)      | NOT NULL     |
| supplierPhone | NVARCHAR(20)       | NULL         |
| supplierEmail | NVARCHAR(254)      | NULL         |

### Inventory

| Column                  | Type               | Constraint   |
| ----------------------- | ------------------ | ------------ |
| inventoryId             | INT, IDENTITY(1,1) | PK, NOT NULL |
| bakeryId                | INT                | FK, NOT NULL |
| supplierId              | INT                | FK, NOT NULL |
| inventoryItemName       | NVARCHAR(120)      | NOT NULL     |
| inventoryItemType       | NVARCHAR(40)       | NOT NULL     |
| inventoryQuantityOnHand | DECIMAL(12,3)      | NOT NULL     |
| inventoryUnitOfMeasure  | NVARCHAR(20)       | NOT NULL     |

### Reward

| Column                | Type               | Constraint   |
| --------------------- | ------------------ | ------------ |
| rewardId              | INT, IDENTITY(1,1) | PK, NOT NULL |
| customerId            | INT                | FK, NOT NULL |
| orderId               | INT                | FK, NOT NULL |
| rewardPointsEarned    | INT                | NOT NULL     |
| rewardTransactionDate | DATETIME2          | NOT NULL     |

### RewardTier

| Column                 | Type               | Constraint   |
| ---------------------- | ------------------ | ------------ |
| rewardTierId           | INT, IDENTITY(1,1) | PK, NOT NULL |
| rewardTierName         | NVARCHAR(30)       | NOT NULL     |
| rewardTierMinPoints    | INT                | NOT NULL     |
| rewardTierMaxPoints    | INT                | NULL         |
| rewardTierDiscountRate | DECIMAL(5,2)       | NULL         |

### BatchInventory

| Column              | Type          | Constraint       |
| ------------------- | ------------- | ---------------- |
| batchId             | INT           | PK, FK, NOT NULL |
| inventoryId         | INT           | PK, FK, NOT NULL |
| quantityUsed        | DECIMAL(12,3) | NOT NULL         |
| unitOfMeasureAtTime | NVARCHAR(20)  | NOT NULL         |
| usageRecordedDate   | DATETIME2     | NOT NULL         |

### Review

| Column              | Type               | Constraint   |
| ------------------- | ------------------ | ------------ |
| reviewId            | INT, IDENTITY(1,1) | PK, NOT NULL |
| customerId          | INT                | FK, NOT NULL |
| productId           | INT                | FK, NOT NULL |
| employeeId          | INT                | FK, NULL     |
| reviewRating        | TINYINT            | NOT NULL     |
| reviewComment       | NVARCHAR(2000)     | NULL         |
| reviewSubmittedDate | DATETIME2          | NOT NULL     |
| reviewStatus        | NVARCHAR(30)       | NOT NULL     |
| reviewApprovalDate  | DATETIME2          | NULL         |

### CustomerPreference

| Column             | Type         | Constraint       |
| ------------------ | ------------ | ---------------- |
| customerId         | INT          | PK, FK, NOT NULL |
| tagId              | INT          | PK, FK, NOT NULL |
| preferenceType     | NVARCHAR(20) | NOT NULL         |
| preferenceStrength | TINYINT      | NULL             |

### Message

| Column              | Type               | Constraint          |
| ------------------- | ------------------ | ------------------- |
| messageId           | INT, IDENTITY(1,1) | PK, NOT NULL        |
| senderId            | INT                | FK, NOT NULL        |
| receiverId          | INT                | FK, NOT NULL        |
| messageSubject      | NVARCHAR(255)      | NOT NULL            |
| messageContent      | NVARCHAR(2000)     | NOT NULL            |
| messageSentDateTime | DATETIME2          | NOT NULL            |
| messageIsRead       | BIT                | NOT NULL, DEFAULT 0 |

---

## Class Relations

| Association                   | Meaning                     | Multiplicity |
| ----------------------------- | --------------------------- | ------------ |
| User ↔ Customer               | User is Customer            | 1..1 ↔ 0..1  |
| User ↔ Employee               | User is Employee            | 1..1 ↔ 0..1  |
| Bakery ↔ Employee             | Bakery employs Employee     | 1..1 ↔ 0..\* |
| Bakery ↔ Batch                | Bakery produces Batch       | 1..1 ↔ 0..\* |
| Product ↔ Batch               | Batch is Product            | 1..1 ↔ 0..\* |
| Employee ↔ Batch              | Employee prepares Batch     | 1..1 ↔ 0..\* |
| Customer ↔ Order              | Customer places Order       | 1..1 ↔ 0..\* |
| Bakery ↔ Order                | Bakery fulfills Order       | 1..1 ↔ 0..\* |
| Order ↔ OrderItem             | Order contains OrderItem    | 1..1 ↔ 1..\* |
| Product ↔ OrderItem           | OrderItem is Product        | 1..1 ↔ 0..\* |
| Batch ↔ OrderItem             | OrderItem uses Batch        | 1..1 ↔ 0..\* |
| Order ↔ Payment               | Order has Payment           | 1..1 ↔ 0..\* |
| Bakery ↔ Inventory            | Bakery tracks Inventory     | 1..1 ↔ 0..\* |
| Supplier ↔ Inventory          | Supplier supplies Inventory | 1..1 ↔ 0..\* |
| Customer ↔ Reward             | Customer earns Reward       | 1..1 ↔ 0..\* |
| Order ↔ Reward                | Order generates Reward      | 1..1 ↔ 0..\* |
| Customer ↔ Review             | Customer writes Review      | 1..1 ↔ 0..\* |
| Product ↔ Review              | Review is for Product       | 1..1 ↔ 0..\* |
| Product ↔ ProductTag          | Product has Tag             | 1..1 ↔ 0..\* |
| Tag ↔ ProductTag              | Tag has Product             | 1..1 ↔ 0..\* |
| Batch ↔ BatchInventory        | Inventory used in Batch     | 1..1 ↔ 0..\* |
| Inventory ↔ BatchInventory    | Batch uses Inventory        | 0.._ ↔ 0.._  |
| Address ↔ Bakery              | Bakery uses Address         | 1..1 ↔ 0..\* |
| Address ↔ Employee            | Employee uses Address       | 1..1 ↔ 0..\* |
| Address ↔ Customer            | Customer uses Address       | 1..1 ↔ 0..\* |
| Address ↔ Order               | Order uses Address          | 1..1 ↔ 0..\* |
| Address ↔ Supplier            | Supplier uses Address       | 1..1 ↔ 0..\* |
| Customer ↔ CustomerPreference | Customer has Preference     | 1..1 ↔ 0..\* |
| Tag ↔ CustomerPreference      | Preference uses Tag         | 0.._ ↔ 0.._  |
| RewardTier ↔ Customer         | Customer has RewardTier     | 1..1 ↔ 0..\* |
| Bakery ↔ BakeryHours          | Bakery has Hours            | 1..1 ↔ 1..\* |
| User ↔ Message (sender)       | User sends Message          | 1..1 ↔ 0..\* |
| User ↔ Message (receiver)     | User receives Message       | 1..1 ↔ 0..\* |

---

## Future Improvements

- Views and different access depending on the type of user.
- Add a table for saving customer payment information (customer's PayPal, credit card, debit, etc.)
- Expand authentication/security support (e.g., password reset tokens, MFA, refresh tokens if required)
- Expand messaging system to support group conversations, attachments, and threaded replies
- Add an audit/logging table (e.g., AuditLog) to track database changes.

---

## Notes

User will be central authority to email and such. Constraints will be added to User such that if `userId` is `NOT NULL`, email comes from User; if `userId` is `NULL`, email comes from `customerEmail`. A bit messy but it should work.
