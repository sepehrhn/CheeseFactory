execute as @e[tag=cheese.barrel] at @s if block ~ ~ ~ barrel{Items:[{Slot:0b,id:"minecraft:suspicious_stew",Count:1b,tag:{curd:1b}}]} run scoreboard players add @s ch.ferment 1

execute as @e[tag=cheese.barrel,scores={ch.ferment=9600..}] at @s run function cheesemaking:cheese/create_cheese


