- make jeamlit run from a maven project with dependencies, 
 a single file, a jbang file, or a gradle project
- add lit - improve the component system
- fix hot reload
- replace System.out.println by proper logging
- let's make this a proper multimodule project

- let's make sure there are not custom component naming collisions 

## Component System v2 Notes

### Current Design (documented in DESIGN_DECISIONS.md)
- register() method: component definition (once per type)
- render() method: HTML instance with data (per render)
- handleEvent() method: process events from frontend
- JSON props communication between frontend/backend

### Next Iteration Improvements Needed:
1. ~~Template system for safe HTML generation~~ (DECISION: keep string-based approach)
2. Per-session component registries (not global static)
3. Typed event handling with annotations
4. Props validation and schemas
5. Component lifecycle management

### Frontend Protocol:
- First render: registration + render
- Subsequent renders: just HTML
- Events back: component_event with componentId, event, props

components are registered - whenever they are registered, they are injected in the frontend
if in prod mode, no need to inject multiple times at each refresh