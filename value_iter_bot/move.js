var fs = require('fs');
var mineflayer = require('../minecraft_api/mineflayer/');
var vec3 = mineflayer.vec3;
var bot = mineflayer.createBot({
  username: "aye_priori",
    password: "password",
});


MAXBLOCKS = 2;

goalX = 143;
goalY = 72;
goalZ = 960;

blockNum = MAXBLOCKS;

actionKillSwitch = false;

bot.on('chat', function(username, message) {
  actionKillSwitch = false;
  switch (message)
  {
    case "F":
      move("forward");
      break;
    case "B":
      move("back");
      break;
    case "L":
      move("left");
      break;
    case "R":
      move("right");
      break;
    case "loc":
      logBotLoc();
      break;
    case "openD":
      openDoor();
      break;
    case "r":
      bot.chat("/tp aye_priori 151.3 74.0 968.5");
      bot.chat("/time set 10000");
      actionKillSwitch = true;
      blockNum = MAXBLOCKS;
      bot.look(0, 0);
      break;
    case "er":
      bot.chat("/tp aye_priori 149.3 72.0 957.5");
      bot.chat("/time set 10000");
      actionKillSwitch = true;
      blockNum = MAXBLOCKS;
      bot.look(0, 0);

      break;
    case "reset":
      bot.chat("/tp aye_priori 150.7 74.0 968.0");
      bot.chat("/time set 10000");
      actionKillSwitch = true;
      blockNum = MAXBLOCKS;
      break;
    case "destroy":
      destroy();
      break;
    case "placeF":
      place("forward");
      break;
    case "placeB":
      place("back");
      break;
    case "placeL":
      place("left");
      break;
    case "placeR":
      place("right");
      break;
    default:
      if (message.slice(0,4) == "plan") {
        planNum = message.slice(4);
        plan = parsePlan(planNum);
        execPlan(plan, 0);
      }
  } 
});

function logBotLoc() {
  botMessage = "curLoc = ( "  + String(Math.floor(bot.entity.position.x)) + ", " + String(Math.floor(bot.entity.position.y)) + ", " + String(Math.floor(bot.entity.position.z)) + " )";
  console.log(botMessage);
}

function isInGoalState() {

  // For readability
  botX = bot.entity.position.x;
  botY = bot.entity.position.y;
  botZ = bot.entity.position.z;

  if (Math.abs(botX - goalX) < 0.6 && Math.abs(botZ - goalZ) < 0.6) {
    // console.log("WOOHOO I MADE IT! :)");
    bot.chat("WOOHOO I MADE IT! :)");
    return true;
  }
  else {
    return false;
  }

}

// Takes as input a world number and returns a list (of strings) of steps to execute
function parsePlan(worldNum) {

  var lines = fs.readFileSync('plan_world' + worldNum + '.p', 'utf8').split(',');
  var arr = new Array();
  for (var l in lines){
      var line = lines[l];
      arr.push(line);
  }
  actionKillSwitch = false;
  return arr;
}

function execPlan(plan, step) {
  // set curr step control state to true

  if (isMoveCommand(plan[step])) {
    bot.setControlState(plan[step], true)
  } else if (plan[step] == "door") {
    openDoor();
  } else if (plan[step] == "jumpF") {
    jump("forward");
  } else if (plan[step] == "placeF") {
    place("forward");
  }

  // Clear control states
  setTimeout(function() {bot.clearControlStates();}, 260)

  // Celebrate and peace out if in goal state
  if (isInGoalState()) {
    return;
  }

  // if not at end of plan, setInterval for next step
  if (step < plan.length) {
    setTimeout(function() {execPlan(plan, step + 1);}, 800)
  }
}

function jump(dir) {
  bot.setControlState(dir, true);
  bot.setControlState("jump", true);
}

function isMoveCommand(action) {
  console.log("[" + action + "]")
  return (action == "right" || action == "left" || action == "forward" || action == "back");
}

function openDoor() {
  rightBlock = bot.blockAt(bot.entity.position.offset(1, 0, 0))
  leftBlock = bot.blockAt(bot.entity.position.offset(-1, 0, 0))
  downBlock = bot.blockAt(bot.entity.position.offset(0, 0, 1))
  upBlock = bot.blockAt(bot.entity.position.offset(0, 0, -1))

  console.log("r: " + rightBlock.name)
  console.log("r: " + upBlock.name)
  console.log("r: " + downBlock.name)
  console.log("r: " + leftBlock.name)

  if (leftBlock.name == "doorWood") {
    bot.activateBlock(leftBlock);
  }
  else if (rightBlock.name == "doorWood") {
    bot.activateBlock(rightBlock);
  }
  else if (upBlock.name == "doorWood") {
    bot.activateBlock(upBlock);
  }
  else if (downBlock.name == "doorWood") {
    bot.activateBlock(downBlock);
  }

  bot.look(0, 0);

}


function canMakeMove(moveDir) {
  var dx = 0;
  var dz = 0;

  // Add 1.5 (instead of 1) here as a hack to make sure we're checking the next block (MC coordinates are extremely imprecise..)
  switch (moveDir)
  {
    case "forward":
      dz = -1;
      break;
    case "back":
      dz = 1;
      break;
    case "left":
      dx = -1;
      break;
    case "right":
      dx = 1;
      break;
  }

  if (bot.blockAt(bot.entity.position.offset(dx, 0, dz)).name != "air" || bot.blockAt(bot.entity.position.offset(dx, -1, dz)).name == "air")  {
    // Movement obstructed or there is a hole
    return false;
  }
  return true;

}

function isBotCommand(cmd) {
// Takes in a string from the agents plan
// returns True if the string corresponds to a movement command and FALSE otherwise (for now, later will add place and dig)
  if (cmd == "forward" || cmd == "back" || cmd == "left" || cmd == "right" || cmd == "placeForward") {
    return true;
  }
  else {
    return false;
  }
}

function move(dir) {
  
  bot.clearControlStates();
  bot.setControlState(dir, true);

  var startX = bot.entity.position.x;
  var startY = bot.entity.position.y;
  var startZ = bot.entity.position.z;

  bot.on('move', movedOne);
  
  function movedOne() {
    if (Math.abs(bot.entity.position.x - startX) >= 1 || Math.abs(bot.entity.position.y - startY) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1) {
      console.log(isInGoalState());
      bot.setControlState(dir, false);
      bot.removeListener('move', movedOne);
    }
  }
}

function destroy() {
  destroyBlock = bot.blockAt(bot.entity.position.offset(0,0,-1));
  if (bot.canDigBlock(destroyBlock)) {
    bot.dig(destroyBlock);
  }
  return;
}

function place(dir) {
  // dir = {"left", "right", "forward", "back"}
  // dist = {1, 2, 3, 4} // TODO: use dist.

  // Bot doesn't have any more blocks!
  if (blockNum  <= 0) {
    return;
  }

  dx = 0;
  dy = 0; // TODO: more elegant way of figuring out dy
  dz = 0;

  if (dir == "left")
    dx = -1.1
  else if (dir == "right")
    dx = 1.1
  else if (dir == "forward")
    dz = -1.1
  else if (dir == "back")
    dz = 1.1

  // Places a block on top of the block directly in front of it.
  placeBlock = bot.blockAt(bot.entity.position.offset(dx,dy,dz));

  console.log(dx,dy)

  // Finds the first non air block in this coordinate "column" that is within the bots "reach" (3)
  while (placeBlock.name == "air") {
    dy = dy - 1;
    placeBlock = bot.blockAt(bot.entity.position.offset(dx,dy,dz));
    if (dy == -2) {
      break;
    }
  }

  x = bot.entity.position.offset(dx,dy,dz)["x"]
  y = bot.entity.position.offset(dx,dy + 1,dz)["y"] // Gets the actual coordinate of the block to be changed 
  z = bot.entity.position.offset(dx,dy,dz)["z"]


  // bot.on('blockUpdate', function(point) {} );

    // Todo: remove this listener
  // bot.on('blockUpdate(' + x + ',' + y + ',' + z + ')',function () {console.log("howdy");}, true);

  // Always placing on the top face (for now)
  placeVec = vec3(placeBlock.position.x, placeBlock.position.y + 1, placeBlock.position.z)
  bot.placeBlock(placeBlock, placeVec);

  blockNum = blockNum - 1;
  return [dx, dy, dz];
}
