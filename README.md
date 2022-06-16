# Description

Application for a Lucky Numbers game and latency check by webscoket.

Client send request with number of players. Game generates a random number from 0 to 999999 for each player and bot. 

For each player and bot constructs a game result with the following rules:
  - counts occurrences of each digit in given number, e.g. for number 447974 there are 4 - 3 times, 7 - 2 times, 9 - one time
  - calculates result for each digit by formula 10^(times-1) * digit, e.g. in number 447974 it will be 10 * 10 * 4 for 4, 10 * 7 for 7, 9 for 9  
  - summarizes all digit result, e.g. for number 447974 it will be 10 * 10 * 4 + 10 * 7 + 9 = 479
  - all results that are below bot player aren't included into result list
  - all winners should be sorted by position

Calculate winning list
- all results that are below bot player aren't included into result list
- all winners are sorted by position


# Protocol description
- play message to websocket
```
{
  "message_type": "request.play",
  "players": 3
}
```
- example results message from websocket
```
{
  "message_type": "response.results",
  "results": [
      {
        "position": 1, 
        "player": "1",
        "number": 966337, 
        "result": 106
      },
      {
        "position": 2, 
        "player": "3",
        "number": 964373, 
        "result": 56
      },
      {
        "position": 3, 
        "player": "2",
        "number": 4283, 
        "result": 17
      }
  ]
}
```

- ping request
```
{
  "id": 5,
  "message_type": "request.ping",
  "timestamp": 1234560
}
```
- pong response
```
{
  "message_type": "response.pong",
  "request_id": 5,
  "request_at": 1234560,
  "timestamp": 1234567
}
```

# Run Server and Client
Server could be run by command `sbt wsServer` 

Client could be run by command `sbt wsClient`. Client provide next functionality:
```
exit -- to exit
ping -- send ping to webserver
game N -- send game request to the server where N is player numbers, f.e. "game 5"
Any other string will be considered as raw json and will be sent to the server
```