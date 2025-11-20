execute as @e[type=item,nbt={Item:{id:"minecraft:wooden_shovel",Count:1b}}] at @s if block ~ ~-0.2 ~ barrel align xz run summon armor_stand ~ ~-0.7 ~ {NoGravity:1b,Invulnerable:1b,ShowArms:1b,Invisible:1b,Tags:["spoon_craft","cheese.barrel"],Pose:{RightArm:[150f,110f,60f]},DisabledSlots:4144959,HandItems:[{id:'minecraft:wooden_shovel',Count:1b},{}]}

execute as @e[type=item,nbt={Item:{id:"minecraft:wooden_shovel",Count:1b}}] at @s if block ~ ~-0.2 ~ barrel run function cheesemaking:cheese/killshovel

execute as @e[type=armor_stand,tag=cheese.barrel] at @s if block ~ ~ ~ air run function cheesemaking:cheese/return_shovel