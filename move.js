var fs = require('fs');
var mineflayer = require('../');
var vec3 = mineflayer.vec3;
var bot = mineflayer.createBot({
  username: "aye_priori",
    password: "password",
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
      botMessage = "curLoc = ( "  + String(Math.floor(bot.entity.position.x)) + ", " + String(Math.floor(bot.entity.position.y)) + ", " + String(Math.floor(bot.entity.position.z)) + " )";
      console.log(botMessage);
      break;
    case "exec":
      plan = parsePlan();
      executePlanStep(plan, 1);
      break;
    case "reset":
      bot.chat("/tp aye_priori 150.5 73.0 968.0");
      break;
  } 
});

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
  
  bot.setControlState(plan[step], true);
  var startX = bot.entity.position.x;
  var startY = bot.entity.position.y;
  var startZ = bot.entity.position.z;
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
