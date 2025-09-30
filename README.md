# PB-Plugins Platform
___
## Manager
___
___
### Description

Менеджер запуска плагинов для платформы `PB-Plugins-Platform`.
___
### Before using

Для использования менеджера - необходимо его подключить как зависимость к своему проекту.

Пакет доступен в центральном репозитории [Maven](https://mvnrepository.com/artifact/io.github.patbattb/pb-plugins-manager).


___
### Usage

___
#### `PluginScheduler`

Основной класс, реализующий функционал циклического запуска плагинов 
на основе их настроек: `PluginScheduler`

`PluginScheduler` принимает в конструкторе:
- `PluginManager` с загруженными плагинами.
- `PluginExecutor` для многопоточного запуска плагинов.

```java
PluginScheduler scheduler = new PluginScheduler(manager, new PluginExecutor(), 1)
```

`PluginScheduler` должен быть закрыт после завершения работы и реализует `AutoCloseable`, 
так что может вызываться в блоке try-with-resources.
___
#### `PluginExecutor`

Запускает задачи в отдельных потоках с предотвращением нескольких одновременных запусков одной задачи.
Если прошлый запуск одной задачи не завершился - следующий ее запуск будет игнорирован.
Тогда как другие задачи могут быть запущены.

Количество потоков можно задать при создании.

```java
int threadPool = 10;
PluginExecutor executor = new PluginExecutor(threadPool);
```
___
#### `PluginManager` 

Производит загрузку, хранение и запуск плагинов.

```java
PluginManager manager = new PluginManager(loader);
```
При создании менеджера необходимо передать загрузчик плагинов, реализующий интерфейс `PluginLoader`.
В сборке реализован загрузчик плагинов из jar-файлов `JarPluginLoader`. При необходимости можно реализовать свой.
___
### `JarPluginLoader`

Дефолтный загрузчик плагинов, реализующий загрузку плагинов в виде jar-файлов.
Jar-файлы для успешной загрузки должны соответствовать спецификации:
- В Jar-сборке должен быть класс, реализующий класс `Plugin` из библиотеки [PB-Plugins-Core](https://github.com/PatBatTB/PB-Plugins-Core).
- Такой класс может быть только один. Его метод `run` является точкой входа в программу.
___

Простой пример использования:
```java
PluginLoader loader = new JarPluginLoader(Path.of("plugins"));
PluginManager manager;
try {
    manager = new PluginManager(loader);
} catch (PluginNotLoadedException e) {
    throw new RuntimeException(e);
}
try (PluginScheduler scheduler = new PluginScheduler(manager, new PluginExecutor(), 1)) {
    scheduler.run();
    System.exit(scheduler.getExitCode());
}
```


