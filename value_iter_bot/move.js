var fs = require('fs');
var mineflayer = require('../minecraft_api/mineflayer/');
var vec3 = mineflayer.vec3;
var bot = mineflayer.createBot({
  username: "aye_priori",
    password: "Sprolls5!",
});

goalX = 143;
goalY = 72;
goalZ = 960;

blockNum = 2;

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
    case "r":
      bot.chat("/tp aye_priori 150.7 74.0 968.0");
      bot.chat("/time set 1000");
      actionKillSwitch = true;
      break;
    case "reset":
      bot.chat("/tp aye_priori 150.7 74.0 968.0");
      bot.chat("/time set 10000");
      actionKillSwitch = true;
      break;
    case "destroy":
      destroy();
      break;
    case "placeF":
      place("forward",1);
      break;
    case "placeB":
      place("back",1);
      break;
    case "placeL":
      place("left",1);
      break;
    case "placeR":
      place("right",1);
      break;
    default:
      if (message.slice(0,4) == "plan") {
        planNum = message.slice(4);
        plan = parsePlan(planNum);
        executePlanStep(plan, 1);
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

  if (Math.floor(botX) == goalX && Math.floor(botY) == goalY && Math.floor(botZ) == goalZ) {
    // console.log("WOOHOO I MADE IT! :)");
    bot.chat("WOOHOO I MADE IT! :)");
    return true;
  }
  else {
    return false;
  }

}

function parsePlan(worldNum) {
  console.log('plan_world' + worldNum + '.plan');
  var lines = fs.readFileSync('plan_world' + worldNum + '.plan', 'utf8').split(',');
  var arr = new Array();
  for (var l in lines){
      var line = lines[l];
      arr.push(line);
  }
  actionKillSwitch = false;
  return arr;
}

function executePlanStep(plan, step) {
  if(actionKillSwitch == true) {
    return;
  }

  var startX = bot.entity.position.x;
  var startY = bot.entity.position.y;
  var startZ = bot.entity.position.z;

  if(plan[step] == "placeForward") {
    place("forward",1);
    movedOne(true);
  }
  else if (canMakeMove(plan[step])) {
    // Movement obstructed or there is a hole.
    bot.setControlState(plan[step], true);
  }
  else {
    // Try the next move, anyways
    console.log("skipped a step" + plan[step]);
    movedOne(true);
  }


  
  bot.on('move', movedOne, false);
  
  function movedOne(skipFlag) {
    if (Math.abs(bot.entity.position.x - startX) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1 || skipFlag == true) {
      isInGoalState()
      if (plan[step] != "placeForward") {
        bot.setControlState(plan[step], false);
        bot.removeListener('move', movedOne);        
      }
      // Calls the next step of the plan, if there is one
      
      console.log(isBotCommand(plan[step + 1]))
      if (step <= plan.length && isBotCommand(plan[step + 1])) {
        executePlanStep(plan, step + 1);
      }
    }
  }
}

function canMakeMove(moveDir) {
  var dx = 0;
  var dz = 0;

  // Add 1.5 (instead of 1) here as a hack to make sure we're checking the next block (MC coordinates are extremely imprecise..)
  switch (moveDir)
  {
    case "forward":
      dz = -1.3;
      break;
    case "back":
      dz = 1.3;
      break;
    case "left":
      dx = -1.3;
      break;
    case "right":
      dx = 1.3;
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

function place(dir, dist) {
  // dir = {"left", "right", "forward", "back"}
  // dist = {1, 2, 3, 4} // TODO: use dist.
  
  if (blockNum  <= 0) {
    return;
  }

  dx = 0;
  dy = 0; // TODO: more elegant way of figuring out dy
  dz = 0;

  if (dir == "left")
    dx = -1
  else if (dir == "right")
    dx = 1
  else if (dir == "forward")
    dz = -1
  else if (dir == "back")
    dz = 1

  dx = dx * dist;
  dz = dz * dist;

  // Places a block on top of the block directly in front of it.
  placeBlock = bot.blockAt(bot.entity.position.offset(dx,dy,dz));
  // Finds the first non air block in this coordinate "column" that is within the bots "reach" (3)
  while (placeBlock.name == "air") {
    if (dy < -3) {
      return ;
    }
    dy = dy - 1;
    placeBlock = bot.blockAt(bot.entity.position.offset(dx,dy,dz));
  }

  placeVec = vec3(placeBlock.position.x, placeBlock.position.y + 1, placeBlock.position.z)
  bot.placeBlock(placeBlock, placeVec);

  blockNum = blockNum - 1;

  return;
}
