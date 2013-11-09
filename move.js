var fs = require('fs');
var mineflayer = require('../');
var vec3 = mineflayer.vec3;
var bot = mineflayer.createBot({
  username: "aye_priori",
    password: "Sprolls5!",
});

goalX = 143;
goalY = 72;
goalZ = 960;

bot.on('chat', function(username, message) {
  
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
    case "exec":
      plan = parsePlan();
      executePlanStep(plan, 1);
      break;
    case "reset":
      bot.chat("/tp aye_priori 150.5 74.0 968.0");
      break;
    case "orient":
      orient();
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

function parsePlan() {
  var lines = fs.readFileSync('plan.p', 'utf8').split(',');
  var arr = new Array();
  for (var l in lines){
      var line = lines[l];
      arr.push(line);
  }
  return arr;
}

function executePlanStep(plan, step) {
  var startX = bot.entity.position.x;
  var startY = bot.entity.position.y;
  var startZ = bot.entity.position.z;

  if (!canMakeMove(plan[step])) {
    // Movement obstructed or there is a hole.
    console.log("can't move!");
    return;
  }


  bot.setControlState(plan[step], true);
  
  bot.on('move', movedOne);
  
  function movedOne() {
    if (Math.abs(bot.entity.position.x - startX) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1) {
      isInGoalState()
      bot.setControlState(plan[step], false);
      bot.removeListener('move', movedOne);
      
      // Calls the next step of the plan, if there is one
      if (step <= plan.length && isBotCommand(plan[step + 1])) {
        executePlanStep(plan, step + 1);
      }
    }
  }
}

function canMakeMove(moveDir) {
  var dx = 0;
  var dz = 0;
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

  if (bot.blockAt(bot.entity.position.offset(dx,0,dz)).name != "air" || bot.blockAt(bot.entity.position.offset(dx,-1,dz)).name == "air")  {
    // Movement obstructed or there is a hole
    return false;
  }
  return true;

}

function isBotCommand(cmd) {
// Takes in a string from the agents plan
// returns True if the string corresponds to a movement command and FALSE otherwise (for now, later will add place and dig)
  if (cmd == "forward" || cmd == "back" || cmd == "left" || cmd == "right") {
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
    if (Math.abs(bot.entity.position.x - startX) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1) {
      console.log(isInGoalState());
      bot.setControlState(dir, false);
      bot.removeListener('move', movedOne);
    }
  }
}

function orient() {
  bot.lookAt(bot.entity.position.offset(1,0,0));
  return;
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
  // dist = {1, 2, 3, 4} // TODO: implement dist.
  
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
  return;
}
