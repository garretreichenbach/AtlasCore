# AtlasContracts

Take on **contracts** for Gold Bars — or post your own. AtlasContracts adds a shared contract
board to the shop window and the top-bar **CONTRACTS** menu. All rewards and costs are paid in
**Gold Bars** (the same physical currency used by the Exchange), not credits.

## Contract types

- **Bounty** — Hunt a target. Player bounties are posted by other players (kill the named target);
  NPC factions also auto-post bounties on players who repeatedly attack them. Mob bounties task you
  with destroying a randomly-spawned pirate group.
- **Items** — Deliver a quantity of a specific item to claim the reward. The items are consumed on
  turn-in.
- **Escort** *(coming soon — currently disabled)* — Protect an auto-spawned cargo convoy along a
  multi-sector route while pirate waves attack. Disabled until trader/cargo blueprints are available;
  a server admin can enable it with `escort_enabled` in the config once they are.

## Posting a contract

Open the **SHOP → CONTRACTS** tab and click **ADD CONTRACT** (you must be in a faction). Choose a
type (**Bounty** or **Items**; **Escort** appears only when enabled), fill in the reward in Gold
Bars, and confirm. The Gold Bars
are taken from your inventory immediately and paid to whoever completes the contract. Cancelling an
unclaimed contract refunds your bars.

## Taking & turning in a contract

- **Accept** a contract from the shop CONTRACTS tab.
- Track and turn in your active contracts from the top-bar **CONTRACTS** menu — the button
  highlights when one is ready to complete.
- Rewards are delivered as Gold Bars. If your inventory is full (or you're offline), the bars are
  held and delivered automatically when there's space / on your next login.

## Admin commands

- `/random_contracts [amount]` — generate random contracts.
- `/list_contracts [player] [type...]` — list a player's active contracts.
- `/purge_contracts <amount|all> [type...]` — remove contracts.
- `/complete_contracts <id|all> [player]` — force-complete contracts.

## Configuration

Reward and cost values are intentionally small (Gold Bars are physical items) and fully configurable
in the AtlasContracts config — including auto-bounty reward, escort base reward, and the minimum/
default player bounty amount.
