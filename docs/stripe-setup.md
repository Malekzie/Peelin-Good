# Stripe Payment Setup

This guide walks through everything required to get Stripe payments working locally, from installing the CLI to running a test transaction through the Android app.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [1. Install the Stripe CLI](#1-install-the-stripe-cli)
- [2. Add the Stripe CLI to Your PATH](#2-add-the-stripe-cli-to-your-path)
- [3. Log In to the Stripe CLI](#3-log-in-to-the-stripe-cli)
- [4. Get Your Webhook Secret](#4-get-your-webhook-secret)
- [5. Configure the Backend (Workshop7)](#5-configure-the-backend-workshop7)
- [6. Configure the Android App (Workshop6)](#6-configure-the-android-app-workshop6)
- [7. Run Everything](#7-run-everything)
- [8. Test a Payment](#8-test-a-payment)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

- Stripe CLI installed
- The Workshop7 backend running locally
- The Workshop6 Android app running in an emulator or on a physical device

---

## 1. Install the Stripe CLI

### Windows

Download the latest Windows release from the [Stripe CLI GitHub releases page](https://github.com/stripe/stripe-cli/releases/latest).

1. Download the file named `stripe_X.X.X_windows_x86_64.zip`.
2. Extract the zip — you will get a single file: `stripe.exe`.
3. Move `stripe.exe` to a permanent folder, for example: `C:\stripe\stripe.exe`.

### macOS

```sh
brew install stripe/stripe-cli/stripe
```

### Linux

```sh
# Debian/Ubuntu
wget -qO- https://packages.stripe.dev/api/security/keypkg/public | sudo gpg --dearmor -o /usr/share/keyrings/stripe.gpg
echo "deb [signed-by=/usr/share/keyrings/stripe.gpg] https://packages.stripe.dev/stripe-cli-debian-local stable main" | sudo tee /etc/apt/sources.list.d/stripe.list
sudo apt update && sudo apt install stripe
```

---

## 2. Add the Stripe CLI to Your PATH

Homebrew (macOS) and the Linux package manager handle this automatically. On Windows you need to do it manually.

### Windows

1. Open **Start** → search for **"Edit the system environment variables"** → click **Environment Variables**.
2. Under **User variables**, select **Path** and click **Edit**.
3. Click **New** and enter the folder where you placed `stripe.exe`, for example:
   ```
   C:\stripe
   ```
4. Click **OK** on all dialogs to save.
5. Open a **new** terminal window and verify the installation:
   ```sh
   stripe --version
   ```
   You should see output like `stripe version 1.x.x`.

---

## 3. Log In to the Stripe CLI

```sh
stripe login
```

This opens a browser window asking you to authorise the CLI with your Stripe account. Click **Allow access**. Once confirmed, the terminal will display:

```
Done! The Stripe CLI is configured for <your@email.com> in test mode.
```

The CLI is now linked to your Stripe account and will use your test keys automatically.

---

## 4. Get Your Webhook Secret

The webhook secret is generated when you start the Stripe listener, not from the dashboard. Run this command (you will keep it running during development):

```sh
stripe listen --forward-to localhost:8080/api/v1/stripe/webhook
```

The first time it runs, it will print something like:

```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx (^C to quit)
```

Copy the `whsec_...` value — that is your **webhook secret**. You will add it to your backend `.env.local` in the next step.

> **Leave this terminal open** while you are testing payments. The listener must be running to forward webhook events from Stripe to your local backend. Every time you restart it, the webhook secret is the same as long as you are logged in to the same Stripe account.

---

## 5. Configure the Backend (Workshop7)

The backend reads Stripe keys from your `.env.local` file.

1. In the `apps/backend/` directory, look for `.env.example`. Create a copy named `.env.local` in the same directory:

   ```sh
   cp apps/backend/.env.example apps/backend/.env.local
   ```

2. Open `apps/backend/.env.local` and add the following three Stripe entries:

   ```env
   STRIPE_SECRET_KEY=sk_test_...
   STRIPE_WEBHOOK_SECRET=whsec_...
   STRIPE_PUBLISHABLE_KEY=pk_test_...
   ```

   | Variable                 | Where to find it                                           |
   | ------------------------ | ---------------------------------------------------------- |
   | `STRIPE_SECRET_KEY`      | Stripe Dashboard → Developers → API keys → Secret key      |
   | `STRIPE_WEBHOOK_SECRET`  | Output of `stripe listen --forward-to ...` (see step 5)    |
   | `STRIPE_PUBLISHABLE_KEY` | Stripe Dashboard → Developers → API keys → Publishable key |

---

## 6. Configure the Android App

The Android app reads the Stripe publishable key from `local.properties`.

1. In the root of the Workshop6 repo, look for `local.properties.example`. Create a copy named `local.properties` in the same directory:

   ```sh
   cp local.properties.example local.properties
   ```

   > If `local.properties` already exists (Android Studio creates it automatically), just open it and add the line below — do not replace the file.

2. Add the following line to `local.properties`:

   ```properties
   stripe.publishable.key=pk_test_...
   ```

   Replace `pk_test_...` with the **Publishable key** from the Stripe Dashboard.

3. **Sync the project** in Android Studio (**File → Sync Project with Gradle Files**) after saving the file.

---

## 7. Run Everything

You need three things running at the same time, each in its own terminal:

**Terminal 1 — Backend**

```sh
cd apps/backend
./mvnw spring-boot:run        # Linux/macOS
mvnw.cmd spring-boot:run      # Windows
```

**Terminal 2 — Stripe webhook listener**

```sh
stripe listen --forward-to localhost:8080/api/v1/stripe/webhook
```

**Terminal 3 — Android emulator / app**

Launch the app from Android Studio.

---

## 8. Test a Payment

1. Log in to the Android app and add items to your cart.
2. Tap **Place Order** on the checkout screen.
3. The Stripe **PaymentSheet** will appear as a bottom sheet — this is a native in-app UI, no browser is involved.
4. Enter one of the Stripe test cards:

   | Card number           | Scenario                                 |
   | --------------------- | ---------------------------------------- |
   | `4242 4242 4242 4242` | Payment succeeds                         |
   | `4000 0000 0000 9995` | Payment is declined (insufficient funds) |
   | `4000 0025 0000 3155` | Requires 3D Secure authentication        |

   Use any future expiry date (e.g. `12/34`) and any 3-digit CVC (e.g. `123`).

5. Tap **Pay** in the PaymentSheet.
6. On success, the app navigates back to the home screen.
7. In Terminal 2, you should see a `payment_intent.succeeded` event arrive and be forwarded to your backend.
8. The backend marks the order as `paid` and awards loyalty points to the customer.

---

## Troubleshooting

### The PaymentSheet does not appear

- Make sure `stripe.publishable.key` is set correctly in `local.properties` and that you have re-synced Gradle.
- Make sure `STRIPE_PUBLISHABLE_KEY` in `.env.local` matches the same Stripe account as your secret key.
- Check logcat in Android Studio for any `PaymentConfiguration` or `Stripe` errors.

### Webhook events are not being received by the backend

- Make sure `stripe listen --forward-to localhost:8080/api/v1/stripe/webhook` is running in its own terminal.
- Make sure the `STRIPE_WEBHOOK_SECRET` in `.env.local` matches the `whsec_...` value printed by the listener.
- Make sure the backend is running and accessible on port `8080`.

### Order status stays at `pending_payment` after a successful payment

- This means the webhook did not reach the backend. Check the two points above.
- You can also replay the last event manually:
  ```sh
  stripe events resend <event_id>
  ```
  The event ID appears in the listener output after each forwarded event.

### `stripe: command not found` (Windows)

- Confirm that the folder containing `stripe.exe` is added to your **User Path** environment variable (see step 3).
- Open a **new** terminal after making the change — existing terminals do not pick up PATH changes.

### I got a new webhook secret and orders are no longer being fulfilled

- The webhook secret only changes if you log out of the CLI or log in with a different Stripe account.
- Copy the new `whsec_...` value from the listener output and update `STRIPE_WEBHOOK_SECRET` in `.env.local`, then restart the backend.
