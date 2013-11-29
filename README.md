# myrtle

A Clojure IDE using your browser (ace editor) and websockets to communicate with nrepl and evaluate code.
Includes other software such as:
	* hot-loading dependencies using 'alembic - github.com/pallet/alembic
	* code-completion suggestions with 'clojure-complete - github.com/ninjudd/clojure-complete
	* 'ace editor, web-based customizable code editor - github.com/ajaxorg/ace-builds/
	* javascript front-end (json and websockets) for communication with myrtle back-end
	* back-end runs on http-kit, ring+compojure

Features include:
	* code-completion suggestions with tab-key
	* console displaying nrepl results and all websocket communication
	* remote evaluation of code


## prereqs

You will need [Leiningen][1] 2.0+ or above installed.

[1]: https://github.com/technomancy/leiningen


## getting things working

Currently myrtle must be included in the project, soon will be standalone with project jack-in ability
Must download ace editor build-source ('noconflict) and place in public folder.



## License

Copyright © 2013 Chris Gill
