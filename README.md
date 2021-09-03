A simple mod that prevents the user from placing water or hay bales while falling. So you
can no longer jump off a cliff and just place water or hay right before you hit the
bottom to nullify fall damage. This makes falling dangerous again, and removes
the incentive to carry a bucket of water or a bale of hay everywhere.

You can configure a list of items, then players right-clicking with those items
won't do anything if they're falling. By default the list has water buckets,
hay bales, and lava buckets.

Some edge cases:

* Lava buckets are on the blacklist too, because otherwise you can actually jump
  off a cliff and survive, if you drink a potion of fire resistance first and
  then place lava at the bottom.
* You cannot place the items while flying.
* You can place the items while floating in water or lava.
* You can place the items while climbing a ladder.
* You can place the items if you're riding a vehicle (horse, minecart, etc.) but
  only if the vehicle is on the ground.
* A strider walking on lava counts as "on the ground", so you can place the
  items while sitting on it.

A related mod to consider is [Surface
Tension](https://www.curseforge.com/minecraft/mc-mods/surface-tension) by
ElenaiDev, which lets you inflict a configurable amount of damage when you fall
a significant distance onto water.

This mod still leaves open the option of placing the water at the top of the cliff,
waiting for it to pour down, then just jumping to the bottom. Surface Tension
fixes that, but still leaves open the option of jumping into the running stream
and swimming down. Not sure how to fix this; maybe make fluid source block move in the
direction they're flowing, if there's only 1 direction, and also make it so
that when water starts flowing directly down, the source block gets moved to
that position and instantly dropped all the way down? Doesn't seem worth the
effort to fix.
