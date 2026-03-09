# Entity and Model Classes

This directory contains JPA entity classes representing database tables and domain objects for the Peelin Bakery Management System.

## Core Entities

### User Management
- **User** - Represents system users with authentication credentials (username, email)
- **Address** - Stores address information for users and businesses
- **Employee** - Represents bakery employees with role and employment detail
- **Customer** - Represents bakery customers with contact and preference information

### Products & Inventory
- **Product** - Represents bakery products with pricing and descriptions
- **Tag** - Represents product tags/categories
- **ProductTag** - Junction table linking products to tags (many-to-many)
- **Inventory** - Tracks product stock levels and availability
- **Batch** - Represents production batches of products
- **BatchInventory** - Links batches to inventory (many-to-many with composite key)

### Orders & Sales
- **Order** - Represents customer orders with status tracking
- **OrderItem** - Line items within orders (one-to-many relationship)
- **Payment** - Represents payment transactions for orders
- **Review** - Customer reviews and ratings for products/orders

### Bakery Management
- **Bakery** - Represents bakery locations/franchises
- **BakeryHour** - Operating hours for bakeries
- **Supplier** - Vendor/supplier information for inventory replenishment
Read
### Customer Relationships
- **CustomerPreference** - Customer preferences for products (many-to-many with composite key)
- **Reward** - Loyalty reward programs
- **RewardTier** - Reward tier levels (bronze, silver, gold, etc.)

### Communication
- **Message** - System messages and notifications

## Composite Key Entities
- **ProductTagId** - Composite key for Product-Tag relationships
- **CustomerPreferenceId** - Composite key for Customer-Product Preference relationships
- **BatchInventoryId** - Composite key for Batch-Inventory relationships
