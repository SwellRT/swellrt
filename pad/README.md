# SwellRT Pad

SwellRT Pad is a web-based collaborative editor, build with [SwellRT real-time technology](http://swellrt.org) and the Angular 2 framework.

## Installation

Install *node.js* and *npm* package manager in order to download the dependencies. Then, do:

```
git clone git://github.com/p2pvalue/swellrt-pad
cd swellrt-pad
npm start
```
By default, it is going to be connected to the SwellRT demo server.

If you want to install your own SwellRT server, please visit the [SwellRt Readme](https://github.com/p2pvalue/swellrt). By now, the URL is hardcoded in several places of the app, i.e. in `index.html`.

*Any issue after updating Angular2 dependencies or another libraries?*

After updating Angular2 libraries or other dependencies you should run following commands to get the app running:

```
rm -R node_modules
rm -R typings
npm install
npm start
```

## Copyright and License

Code and documentation copyright 2016 [Pablo Ojanguren](https://github.com/pablojan) and [David Llop](https://github.com/llopv). Code released under the Affero GPL v3 license, and docs released under the GNU Free Documentation License.
