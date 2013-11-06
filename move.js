var fs = require('fs');
var mineflayer = require('../');
var vec3 = mineflayer.vec3;
var bot = mineflayer.createBot({
  username: "aye_priori",
    password: "Sprolls5!",
});

EventEmitter.setMaxListeners(20);

isMoving = false; // Flag to indicate if the bot is currently moving


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
      executePlan(plan);
      break;
  } 
});

function parsePlan() {
  var lines = fs.readFileSync('plan.p', 'utf8').split(',');
  var arr = new Array();
  for (var l in lines){
      var line = lines[l];
      arr.push(line);
  }
  return arr;
}

function executePlan(plan) {
    for (var i=0;i<plan.length;i++) {
      setTimeout(executePlanStep(i), 500*i);
    }
}

function executePlanStep(i) {
      console.log(i)
      if (plan[i] == "destroyForward") {
        // Do destroy stuff
        console.log("destroy")
      }
      else if (plan[i] == "placeForward") {
        // Do destroy stuff
        console.log("place")
      }
      else if (plan[i] == "forward" || plan[i] == "back" || plan[i] == "left" || plan[i] == "right") {
        move(plan[i])
      }
      else {
        console.log("Got odd command: " + plan[i])
      }
}


function move(dir) {
  
  bot.setControlState(dir, true);
  isMoving = true;

  var startX = bot.entity.position.x;
  var startY = bot.entity.position.y;
  var startZ = bot.entity.position.z;
  console.log(bot.entity.position)
  bot.on('move', movedOne);
  
  function movedOne() {
    if (Math.abs(bot.entity.position.x - startX) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1) {
      bot.setControlState(dir, false);
      isMoving = false;
      bot.removeListener('move', movedOne);
    }
  }
}