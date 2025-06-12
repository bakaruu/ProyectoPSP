# Proyecto PSP – Chat Swing
 
Este proyecto implementa un chat cliente-servidor...


## Compatibilidad Multiplataforma de ChatApp

## Descripción General

Este proyecto consta de dos componentes escritos en Java:

* **Servidor**: aplicación de consola (ChatServer + HandlerCliente) sin dependencia gráfica.
* **Cliente**: aplicación GUI basada en **Java Swing**.

Gracias a la JVM, ambos módulos funcionan de forma idéntica en distintos sistemas operativos, cumpliendo el requisito de ejecutar la UI en Windows/Linux con X y el servidor en Ubuntu Server sin X.

---

## 1. Requisitos Previos

* **Java 17+** (JRE o JDK) instalado en todas las máquinas.
* **Conexión de red** abierta entre cliente(s) y servidor (puerto configurable, por defecto 12345).

---

## 2. Descarga desde GitHub

1. Accede al repositorio en GitHub y haz clic en “Code” → “Download ZIP”.
2. Descomprime el archivo ZIP en tu máquina:

   ```bash
   unzip proyectoPSP-master.zip -d proyectoPSP
   cd proyectoPSP
   ```

## 3. Compilar y Empaquetar

Dentro de la carpeta `proyectoPSP` ya descomprimida, ejecuta:

```bash
./gradlew clean jar
```

Esto generará:

* `server/build/libs/chat-server.jar`
* `client/build/libs/chat-client.jar`

---

## 4. Ejecutar el Servidor (Ubuntu Server sin X) Ejecutar el Servidor (Ubuntu Server sin X)

1. Subir `chat-server.jar` al servidor remoto (por ejemplo via `scp`).
2. Acceder por SSH y ejecutar en modo consola:

   ```bash
   java -jar chat-server.jar 12345
   ```
3. Verificar en el prompt que el servidor muestra: `Servidor iniciado en puerto 12345`.

> No se requiere entorno gráfico ni variables de DISPLAY.

---

## 4. Ejecutar el Cliente con UI

### 4.1. Windows

1. Descargar o copiar `chat-client.jar`.
2. Doble clic o via CMD:

   ```bat
   java -jar chat-client.jar
   ```
3. En la ventana de login, introducir:

   * Nick
   * Avatar (archivo local)
   * IP del servidor (ej. `192.0.2.10`)
   * Puerto (12345)

### 4.2. Linux Desktop (con X)

1. Asegurarse de tener un gestor de ventanas activo.
2. Instalar Java:

   ```bash
   sudo apt update && sudo apt install openjdk-17-jre
   ```
3. Ejecutar:

   ```bash
   java -jar chat-client.jar
   ```
4. Proceder al login como en Windows.

---

## 5. Opciones de Prueba

* **Multicliente local**: ejecutar múltiples instancias de `chat-client.jar` en la misma máquina y conectarse a `localhost:12345`.
* **Cliente remoto**: lanzar el servidor en Ubuntu Server y conectar clientes desde distintos PCs en red.
* **Stress Test**: abrir varios clientes simultáneos para validar concurrencia.

---

## 6. Conclusión

La arquitectura basada en **Java** y la separación clara entre servidor (consola) y cliente (Swing) garantiza:

* **Portabilidad**: mismo bytecode en Windows, Linux Desktop y Ubuntu Server.
* **Simplicidad de despliegue**: solo se necesita un único `.jar` por módulo.
* **Cumplimiento**: punto 7 del enunciado cubierto en su totalidad.

---

*Este README muestra la compatibilidad multiplataforma de la aplicación.*

