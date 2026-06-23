# AtlasBanking

## Overview

AtlasBanking gives every player a personal bank account: a persistent credit
balance, deposits and withdrawals between your bank and your in-game wallet,
player-to-player transfers, and a full transaction history.

## Opening the Bank

Click the **BANKING** button in the top-right player menu, or press the
**Open Banking Menu** key (default **N**). You can rebind it under
*Options → Controls → MOD CONTROLS* in the in-game settings.

## Features

### Account Balance

Your stored credit balance is shown at the top of the banking window.
Balances are whole credits and persist across sessions and server restarts.

### Deposits & Withdrawals

Move credits between your bank account and your in-game wallet:

- **Deposit** — transfers credits from your wallet into your bank.
- **Withdraw** — transfers credits from your bank back into your wallet.

Both operations are validated and applied by the server, so they work the same
in single-player and on multiplayer servers.

### Player Transfers

Use **Send Credits** to transfer credits from your bank to another player's
bank account. Enter the recipient's name, an amount, and an optional subject and
message. The server verifies you have the funds before completing the transfer,
and records it in both players' histories.

### Transaction History

Review past deposits, withdrawals, and transfers, each with a subject, the other
party, and a timestamp.

> **Note:** All balance changes are server-authoritative. The bank never trusts
> the client for amounts or identities, so balances cannot be edited locally.
