var mineflayer = require('../');
var vec3 = mineflayer.vec3;
var bot = mineflayer.createBot({
  username: "aye_priori",
    password: "Sprolls5!",
});

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
    case "O":
      orient();
      break;
    case "loc":
      botMessage = "curLoc = ( "  + String(Math.floor(bot.entity.position.x)) + ", " + String(Math.floor(bot.entity.position.y)) + ", " + String(Math.floor(bot.entity.position.z)) + " )";
      bot.chat(botMessage);
      break;
    case "P":
      place();
      break;
  } 
});

// Called on game init.
bot.on('login', function() {
  bot.chat("/ci"); // Clears Inventory
  bot.chat("/give aye_priori 3 2"); // Gives 2 blocks of dirt
  bot.chat("/time set 10"); // Sets time for lighting
  bot.chat("/weather clear 999999"); // Makes weather clear so we don't get rained on.
});


function orient() {
  return;
  //bot.lookAt(Math.floor(bot.entity.position.offset(0, 1, 1)));
}

function place() {
  return;
  //bot.lookAt(Math.floor(bot.entity.position.offset(0, 1, 1)));
}


function move(dir) {
  bot.setControlState(dir, true);

  var startX = bot.entity.position.x;
  var startY = bot.entity.position.y;
  var startZ = bot.entity.position.z;

  bot.on('move', movedOne);
  
  function movedOne() {
    if (Math.abs(bot.entity.position.x - startX) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1 || Math.abs(bot.entity.position.z - startZ) >= 1) {
      bot.setControlState(dir, false);
      bot.removeListener('move', movedOne);
    }
  }
}