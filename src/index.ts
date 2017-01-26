import * as server from "./server";

const nectarString =
`
_|      _|                        _|
_|_|    _|    _|_|      _|_|_|  _|_|_|_|    _|_|_|  _|  _|_|
_|  _|  _|  _|_|_|_|  _|          _|      _|    _|  _|_|
_|    _|_|  _|        _|          _|      _|    _|  _|
_|      _|    _|_|_|    _|_|_|      _|_|    _|_|_|  _|

`;

console.log(nectarString);

var s: server.Server = new server.Server();
s.run();
