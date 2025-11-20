execute as @e[tag=spoon_craft] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:wooden_shovel",Count:1b}},sort=nearest,limit=1]
execute as @e[tag=spoon_craft] at @s run playsound minecraft:block.bamboo.break neutral @a[distance=..6]
execute as @e[tag=spoon_craft] at @s run data merge block ~ ~ ~ {CustomName:'{"text":"Cheese Barrel"}'}
execute as @e[tag=spoon_craft] run tag @s remove shovel_craft
