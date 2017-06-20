### June 19, 2017

- Removed unused code from alpha version.

- Moved constants to `swell.Constants.*`.

Changed symbol *swellrt* to *swell* to avoid misspellings using the client

- Renamed `swellrt` main object to `swell`.
- Renamed `onReady()` method to `ready()`.
- Renamed `__swellrt_config` to `__swell_config`.
- Renamed `__swellrt_config` to `__swell_config`.
- Renamed `__swellrt_editor_config` to `__swell_editor_config`.

### June 18, 2017

- Argument changed: `swellrt.open()` method returns a promise or callback with passing the opened object as single argument.

- Method to property: `object.id` (Javascript) the object's id is now accessible as property.

- New method (Javascript): `object.get('property').js()`  

- New method (Javascript): `object.get('property').json()`

- Method renamed: `getNode()` to `node()` in map and lists. 