execute as @e[tag=coag_craft] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:milk_bucket",Count:1b}},sort=nearest,limit=1]
execute as @e[tag=coag_craft] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:melon_seeds",Count:1b,tag:{bacteria:1b}}},sort=nearest,limit=1]
execute as @e[tag=coag_craft] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:bowl",Count:1b}},sort=nearest,limit=1]
execute as @e[tag=coag_craft] at @s run playsound minecraft:block.brewing_stand.brew ambient @a[distance=..6]
execute as @e[tag=coag_craft] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bucket",Count:1b}} 
execute as @e[tag=coag_craft] run tag @s remove coag_craft