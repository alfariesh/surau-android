# `:benchmarks`

## Module dependency graph

<!--region graph-->
```mermaid
---
config:
  layout: elk
  elk:
    nodePlacementStrategy: SIMPLE
---
graph TB
  subgraph :feature
    direction TB
    subgraph :feature:settings
      direction TB
      :feature:settings:api[api]:::android-library
      :feature:settings:impl[impl]:::android-library
    end
    subgraph :feature:auth
      direction TB
      :feature:auth:api[api]:::android-library
      :feature:auth:impl[impl]:::android-library
    end
    subgraph :feature:quran
      direction TB
      :feature:quran:api[api]:::android-library
      :feature:quran:impl[impl]:::android-library
    end
  end
  subgraph :sync
    direction TB
    :sync:work[work]:::android-library
  end
  subgraph :core
    direction TB
    :core:analytics[analytics]:::android-library
    :core:common[common]:::jvm-library
    :core:data[data]:::android-library
    :core:database[database]:::android-library
    :core:datastore[datastore]:::android-library
    :core:datastore-proto[datastore-proto]:::jvm-library
    :core:designsystem[designsystem]:::android-library
    :core:domain[domain]:::android-library
    :core:media[media]:::android-library
    :core:model[model]:::jvm-library
    :core:navigation[navigation]:::android-library
    :core:network[network]:::android-library
    :core:ui[ui]:::android-library
  end
  :benchmarks[benchmarks]:::android-test
  :app[app]:::android-application

  :app -.->|baselineProfile| :benchmarks
  :app -.-> :core:analytics
  :app -.-> :core:common
  :app -.-> :core:data
  :app -.-> :core:designsystem
  :app -.-> :core:media
  :app -.-> :core:model
  :app -.-> :core:navigation
  :app -.-> :core:ui
  :app -.-> :feature:auth:api
  :app -.-> :feature:auth:impl
  :app -.-> :feature:quran:api
  :app -.-> :feature:quran:impl
  :app -.-> :feature:settings:impl
  :app -.-> :sync:work
  :benchmarks -.->|testedApks| :app
  :core:data -.-> :core:analytics
  :core:data --> :core:common
  :core:data --> :core:database
  :core:data --> :core:datastore
  :core:data --> :core:network
  :core:database --> :core:model
  :core:datastore -.-> :core:common
  :core:datastore --> :core:datastore-proto
  :core:datastore --> :core:model
  :core:domain --> :core:data
  :core:domain --> :core:model
  :core:media --> :core:model
  :core:network --> :core:common
  :core:network --> :core:model
  :core:ui --> :core:analytics
  :core:ui --> :core:designsystem
  :core:ui --> :core:model
  :feature:auth:api --> :core:navigation
  :feature:auth:impl -.-> :core:data
  :feature:auth:impl -.-> :core:designsystem
  :feature:auth:impl -.-> :core:domain
  :feature:auth:impl -.-> :core:model
  :feature:auth:impl -.-> :core:ui
  :feature:auth:impl --> :feature:auth:api
  :feature:quran:api --> :core:navigation
  :feature:quran:impl -.-> :core:data
  :feature:quran:impl -.-> :core:designsystem
  :feature:quran:impl -.-> :core:domain
  :feature:quran:impl -.-> :core:media
  :feature:quran:impl -.-> :core:model
  :feature:quran:impl -.-> :core:ui
  :feature:quran:impl --> :feature:quran:api
  :feature:settings:api --> :core:navigation
  :feature:settings:impl -.-> :core:data
  :feature:settings:impl -.-> :core:designsystem
  :feature:settings:impl -.-> :core:model
  :feature:settings:impl -.-> :core:ui
  :feature:settings:impl --> :feature:settings:api
  :sync:work -.-> :core:analytics
  :sync:work -.-> :core:data

classDef android-application fill:#CAFFBF,stroke:#000,stroke-width:2px,color:#000;
classDef android-feature fill:#FFD6A5,stroke:#000,stroke-width:2px,color:#000;
classDef android-library fill:#9BF6FF,stroke:#000,stroke-width:2px,color:#000;
classDef android-test fill:#A0C4FF,stroke:#000,stroke-width:2px,color:#000;
classDef jvm-library fill:#BDB2FF,stroke:#000,stroke-width:2px,color:#000;
classDef unknown fill:#FFADAD,stroke:#000,stroke-width:2px,color:#000;
```

<details><summary>📋 Graph legend</summary>

```mermaid
graph TB
  application[application]:::android-application
  feature[feature]:::android-feature
  library[library]:::android-library
  jvm[jvm]:::jvm-library

  application -.-> feature
  library --> jvm

classDef android-application fill:#CAFFBF,stroke:#000,stroke-width:2px,color:#000;
classDef android-feature fill:#FFD6A5,stroke:#000,stroke-width:2px,color:#000;
classDef android-library fill:#9BF6FF,stroke:#000,stroke-width:2px,color:#000;
classDef android-test fill:#A0C4FF,stroke:#000,stroke-width:2px,color:#000;
classDef jvm-library fill:#BDB2FF,stroke:#000,stroke-width:2px,color:#000;
classDef unknown fill:#FFADAD,stroke:#000,stroke-width:2px,color:#000;
```

</details>
<!--endregion-->
