# 职业选择GUI测试报告

**生成时间**：2026-05-22 20:38:36
**源文件哈希**：4d065f0ac7024b74584c72e975da0e2b4470d92219bfae9dbfdf5a1e2b712da0
**测试文件哈希**：f7795270968544f6854acaa93465cc8f4df91a6bbc9cbac9573c4f94e361ce13

## 测试概要

|指标|数值|
|---|---|
|测试总数|8|
|通过数|8|
|失败数|0|
|跳过数|0|

## 测试用例列表

|测试方法|显示名|结果|
|---|---|---|
|聊天栏应输出：正在进入暮澜纪元MMORPG服务器...|聊天栏应输出：正在进入暮澜纪元MMORPG服务器...|✅|
|英文玩家世界选择标题应显示英文|英文玩家世界选择标题应显示英文|✅|
|聊天栏应输出：你已将专精更改为【奥能法师】！|聊天栏应输出：你已将专精更改为【奥能法师】！|✅|
|英文玩家选择世界应看到英文提示|英文玩家选择世界应看到英文提示|✅|
|聊天栏应输出：选择你的世界|聊天栏应输出：选择你的世界|✅|
|聊天栏应输出：你选择了【魔法世界】的力量|聊天栏应输出：你选择了【魔法世界】的力量|✅|
|英文玩家应看到：Entering Mulan Epoch MMORPG Server...|英文玩家应看到：Entering Mulan Epoch MMORPG Server...|✅|
|英文玩家职业已更改应看到英文提示|英文玩家职业已更改应看到英文提示|✅|

## 玩家聊天栏实际输出

> 以下为每个测试用例中玩家聊天栏的实际输出文本（由TestReporter发布）

|测试方法|预期输出|实际输出|
|---|---|---|
|聊天栏应输出：正在进入暮澜纪元MMORPG服务器...|正在进入暮澜纪元MMORPG服务器...|正在进入[暮澜纪元]MMORPG服务器...|
|英文玩家世界选择标题应显示英文|Choose Your World|========== Choose Your World ==========|
|聊天栏应输出：你已将专精更改为【奥能法师】！|你已将专精更改为【奥能法师】！|=== 你已将专精更改为【奥能法师】！ ===|
|英文玩家选择世界应看到英文提示|You have chosen the power of Arcane World|=== You have chosen the power of【Arcane World】and automatically selected the initial specialization【Arcane Mage】! ===|
|聊天栏应输出：选择你的世界|选择你的世界|========== 选择你的世界 ==========|
|聊天栏应输出：你选择了【魔法世界】的力量|你选择了【魔法世界】的力量|=== 你选择了【魔法世界】的力量，并自动选择了初始专精【奥能法师】！ ===|
|英文玩家应看到：Entering Mulan Epoch MMORPG Server...|Entering Mulan Epoch MMORPG Server...|Entering[MulanEpoch]MMORPG Server...|
|英文玩家职业已更改应看到英文提示|You have changed your specialization to Arcane Mage|=== You have changed your specialization to【Arcane Mage】! ===|
