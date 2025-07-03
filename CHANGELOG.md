# Changelog

## [0.1.0](https://github.com/uexodus/arkinstall/compare/v0.0.1...v0.1.0) (2025-07-03)


### Features

* **parted:** Add a PartedExceptionHandler to handle exception thrown directly by libparted. ([758bdf1](https://github.com/uexodus/arkinstall/commit/758bdf1091aaed8f4657d51436f3154852698134))
* **parted:** add basic PartedPartition class ([4384e04](https://github.com/uexodus/arkinstall/commit/4384e04895ce169a6e44b90b324558d15bc7a70c))
* **parted:** add DiskBounds class to validate partitions ([dafaa2a](https://github.com/uexodus/arkinstall/commit/dafaa2aa5de5c0268e94449bd1c53e4702df8891))
* **parted:** add PartedPartition.new for raw partition creation ([842de97](https://github.com/uexodus/arkinstall/commit/842de97ff379eaec78628402072cb10d57f8c554))
* **parted:** add PartedPartition.new for raw partition creation ([ca8d45a](https://github.com/uexodus/arkinstall/commit/ca8d45ac2669dd048f0a8ba05ae6cd9b1256a041))
* **parted:** add partitions field to PartedDisk object ([b58194f](https://github.com/uexodus/arkinstall/commit/b58194f2300bd45fba4ad1943278528b7a580ea4))
* **parted:** Added ConsoleLogger and error logs can only be created using an exception ([a4836c4](https://github.com/uexodus/arkinstall/commit/a4836c48d09eb3721962ca409297dab47774572b))
* **parted:** create PartedDiskBuilder for easy disk creation ([40359ec](https://github.com/uexodus/arkinstall/commit/40359ec10f69eecfe12ddba448649a3b7b25f7f1))
* **parted:** created basic PartedGeometry wrapper of PedGeometry struct ([1148a8c](https://github.com/uexodus/arkinstall/commit/1148a8c5e8491c5d46e3d4064b21503bda0a60b5))
* **parted:** implement createDisk() and commit() for PartedDevice and PartedDisk ([1794f18](https://github.com/uexodus/arkinstall/commit/1794f189827de3425e4becd8de1851ccc00d66e8))
* **parted:** overhaul SafeCPointer usage & functionality. ([02e8b6f](https://github.com/uexodus/arkinstall/commit/02e8b6f3f06b7961d6ebc8e067591bf693ae85d5))


### Bug Fixes

* **parted:** partition pointers should be children of the disk pointer ([05e4cef](https://github.com/uexodus/arkinstall/commit/05e4cefddd660c3c9bac5a84a286752cf71e62a3))
* **parted:** restructure Parted wrappers to use a sealed class ownership model ([2aa8c16](https://github.com/uexodus/arkinstall/commit/2aa8c16b3565aa63a192ed88e2e131db7e7512b7))
