# Changelog

All notable changes to this project will be documented in this file.  
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project tries to adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased Changes]

### Fixed

- Sodium 0.8.x compat: `OcclusionCuller.Visitor` was promoted to a top-level
  interface (`RenderSectionVisitor`), and `Viewport.isBoxVisible`'s old
  6-float AABB signature was split into `isBoxVisible(int,int,int)` and
  `isBoxVisibleDirect(float,float,float,float)`.
- Portals rendering through occluding geometry (or falling back to
  compatibility mode) with a shaderpack active, caused by the deferred
  framebuffer's depth-stencil format not matching Iris's actual main
  render target format.
- Framebuffer thrashing/stutter caused by resizing the deferred
  framebuffer array every time the lag-adaptive portal layer count
  toggled.
- Severe stutter (frame time spikes to 900ms+) when a portal is visible
  with Distant Horizons installed, caused by DH doing synchronous LOD
  generation for the portal's alternate-world view every frame.

## [6.0.7] - 2025-06-18

### Fixed

- Oritech animations not displaying ([#13](https://github.com/iPortalTeam/ImmersivePortalsModForNeo/issues/13)).
- ComputerCraft monitors not displaying anything ([#31](https://github.com/iPortalTeam/ImmersivePortalsModForNeo/issues/31)).

## [6.0.6] - 2024-12-22

### Updated

- Sync upstream (v6.0.6)
- Sodium compat (v0.6.0)
- Iris compat (v1.8.0) (experimental)

### Fixed

- Default config values being wrong

## [6.0.3] - 2024-10-20

### Added

- Initial port to NeoForge 1.21.1

### Known Issues

- Iris compatibility is not fully functional
- Crash with SecurityCraft

[Unreleased Changes]: https://github.com/iPortalTeam/ImmersivePortalsModForNeo/compare/v6.0.7...HEAD
[6.0.7]: https://github.com/iPortalTeam/ImmersivePortalsModForNeo/releases/tag/v6.0.7
[6.0.6]: https://github.com/iPortalTeam/ImmersivePortalsModForNeo/releases/tag/v6.0.6
[6.0.3]: https://github.com/iPortalTeam/ImmersivePortalsModForNeo/releases/tag/v6.0.3

