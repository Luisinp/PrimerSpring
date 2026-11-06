-- =====================================================================
-- TravelZen - Base de datos v2 (MariaDB / MySQL 8)
--
-- Identidad:
--   usuarios        = solo lo necesario para entrar (correo, hash, activo, rol)
--   administradores = datos personales + id_usuario (PK y FK)
--   agentes         = datos personales + id_usuario (PK y FK)
--   clientes        = datos personales + correo de contacto + cuenta OPCIONAL
-- =====================================================================

DROP DATABASE IF EXISTS travelzen;
CREATE DATABASE travelzen CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travelzen;

-- ---------------------------------------------------------------------
-- 1. IDENTIDAD Y ACCESO
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id_rol       INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(30)  NOT NULL UNIQUE,      -- ADMIN, AGENTE, CLIENTE
    descripcion  VARCHAR(150)
) ENGINE=InnoDB;

CREATE TABLE usuarios (
    id_usuario          INT AUTO_INCREMENT PRIMARY KEY,
    correo              VARCHAR(100) NOT NULL UNIQUE,   -- es el usuario del login
    contrasena_cifrada  VARCHAR(100) NOT NULL,          -- BCrypt
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    id_rol              INT          NOT NULL,
    fecha_creacion      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso       DATETIME     NULL,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
) ENGINE=InnoDB;

CREATE TABLE administradores (
    id_usuario           INT PRIMARY KEY,               -- PK y FK a la vez (herencia)
    nombre_completo      VARCHAR(120) NOT NULL,
    documento_identidad  VARCHAR(30)  NOT NULL UNIQUE,
    telefono             VARCHAR(25),
    sexo                 ENUM('M','F'),
    nacionalidad         VARCHAR(60),
    fecha_nacimiento     DATE,
    fecha_ingreso        DATE NOT NULL,
    CONSTRAINT fk_admin_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE agentes (
    id_usuario           INT PRIMARY KEY,               -- PK y FK a la vez (herencia)
    nombre_completo      VARCHAR(120) NOT NULL,
    documento_identidad  VARCHAR(30)  NOT NULL UNIQUE,
    telefono             VARCHAR(25),
    sexo                 ENUM('M','F'),
    nacionalidad         VARCHAR(60),
    fecha_nacimiento     DATE,
    fecha_ingreso        DATE NOT NULL,
    CONSTRAINT fk_agente_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE clientes (
    id_cliente           INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo      VARCHAR(120) NOT NULL,
    documento_identidad  VARCHAR(30)  NOT NULL UNIQUE,
    correo               VARCHAR(100) NOT NULL,         -- contacto (cliente de mostrador no tiene cuenta)
    telefono             VARCHAR(25),
    sexo                 ENUM('M','F'),
    nacionalidad         VARCHAR(60),
    fecha_nacimiento     DATE,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    id_usuario           INT          NULL UNIQUE,      -- NULL = sin cuenta web
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. CATÁLOGO TURÍSTICO (Sprint 1)
-- ---------------------------------------------------------------------
CREATE TABLE destinos (
    id_destino   INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    pais         VARCHAR(60)  NOT NULL,
    descripcion  TEXT,
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_destino UNIQUE (nombre, pais)
) ENGINE=InnoDB;

CREATE TABLE paquetes_turisticos (
    id_paquete           INT AUTO_INCREMENT PRIMARY KEY,
    nombre               VARCHAR(120)  NOT NULL,
    id_destino           INT           NOT NULL,
    duracion_dias        INT           NOT NULL,
    descripcion          TEXT,
    servicios_incluidos  TEXT,
    precio               DECIMAL(10,2) NOT NULL,
    cupos_disponibles    INT           NOT NULL DEFAULT 0,
    imagen_portada       VARCHAR(255),                  -- ruta de la foto del catálogo
    publicado            BOOLEAN       NOT NULL DEFAULT FALSE,
    activo               BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_paquete_destino FOREIGN KEY (id_destino) REFERENCES destinos(id_destino),
    CONSTRAINT ck_paquete_precio  CHECK (precio > 0),
    CONSTRAINT ck_paquete_dias    CHECK (duracion_dias > 0),
    CONSTRAINT ck_paquete_cupos   CHECK (cupos_disponibles >= 0)
) ENGINE=InnoDB;

CREATE TABLE vuelos (
    id_vuelo             INT AUTO_INCREMENT PRIMARY KEY,
    aerolinea            VARCHAR(80)   NOT NULL,
    origen               VARCHAR(80)   NOT NULL,
    destino              VARCHAR(80)   NOT NULL,
    fecha                DATE          NOT NULL,
    hora                 TIME          NOT NULL,
    tarifa               DECIMAL(10,2) NOT NULL,
    asientos_disponibles INT           NOT NULL,
    activo               BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_vuelo_tarifa   CHECK (tarifa >= 0),
    CONSTRAINT ck_vuelo_asientos CHECK (asientos_disponibles >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. RESERVAS
--    id_usuario_registro: quién la registró (cliente o personal)
--    id_agente / id_administrador: quién la controla (solo personal, puede estar vacío)
-- ---------------------------------------------------------------------
CREATE TABLE reservas (
    id_reserva           INT AUTO_INCREMENT PRIMARY KEY,
    codigo_unico         VARCHAR(20)   NOT NULL UNIQUE,
    tipo                 ENUM('VUELO','PAQUETE') NOT NULL,
    fecha_registro       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto_total          DECIMAL(10,2) NOT NULL,
    estado               ENUM('PENDIENTE','CONFIRMADA','CANCELADA','FINALIZADA')
                         NOT NULL DEFAULT 'PENDIENTE',
    estado_pago          ENUM('PENDIENTE','PARCIAL','COMPLETADO')
                         NOT NULL DEFAULT 'PENDIENTE',
    id_cliente           INT NOT NULL,
    id_usuario_registro  INT NOT NULL,
    id_agente            INT NULL,
    id_administrador     INT NULL,
    id_vuelo             INT NULL,
    id_paquete           INT NULL,
    CONSTRAINT fk_reserva_cliente  FOREIGN KEY (id_cliente)          REFERENCES clientes(id_cliente),
    CONSTRAINT fk_reserva_registro FOREIGN KEY (id_usuario_registro) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_reserva_agente   FOREIGN KEY (id_agente)           REFERENCES agentes(id_usuario),
    CONSTRAINT fk_reserva_admin    FOREIGN KEY (id_administrador)    REFERENCES administradores(id_usuario),
    CONSTRAINT fk_reserva_vuelo    FOREIGN KEY (id_vuelo)            REFERENCES vuelos(id_vuelo),
    CONSTRAINT fk_reserva_paquete  FOREIGN KEY (id_paquete)          REFERENCES paquetes_turisticos(id_paquete),
    CONSTRAINT ck_reserva_monto    CHECK (monto_total >= 0),
    CONSTRAINT ck_reserva_tipo     CHECK (
        (tipo = 'VUELO'   AND id_vuelo   IS NOT NULL AND id_paquete IS NULL) OR
        (tipo = 'PAQUETE' AND id_paquete IS NOT NULL AND id_vuelo   IS NULL)
    ),
    CONSTRAINT ck_reserva_control  CHECK (id_agente IS NULL OR id_administrador IS NULL)
) ENGINE=InnoDB;

CREATE TABLE itinerarios (
    id_itinerario  INT AUTO_INCREMENT PRIMARY KEY,
    id_reserva     INT          NOT NULL UNIQUE,
    origen         VARCHAR(80)  NOT NULL,
    destino        VARCHAR(80)  NOT NULL,
    fecha_inicio   DATE         NOT NULL,
    fecha_fin      DATE         NOT NULL,
    CONSTRAINT fk_itinerario_reserva FOREIGN KEY (id_reserva)
        REFERENCES reservas(id_reserva) ON DELETE CASCADE,
    CONSTRAINT ck_itinerario_fechas CHECK (fecha_fin >= fecha_inicio)
) ENGINE=InnoDB;

CREATE TABLE pasajeros (
    id_pasajero          INT AUTO_INCREMENT PRIMARY KEY,
    id_reserva           INT          NOT NULL,
    nombre_completo      VARCHAR(120) NOT NULL,
    documento_identidad  VARCHAR(30)  NOT NULL,
    nacionalidad         VARCHAR(60),
    sexo                 ENUM('M','F'),                 -- los boletos internacionales lo piden
    fecha_nacimiento     DATE,
    CONSTRAINT fk_pasajero_reserva FOREIGN KEY (id_reserva)
        REFERENCES reservas(id_reserva) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE historial_estado_reserva (
    id_historial     INT AUTO_INCREMENT PRIMARY KEY,
    id_reserva       INT NOT NULL,
    id_usuario       INT NOT NULL,                      -- quién hizo el cambio
    estado_anterior  VARCHAR(20) NULL,
    estado_nuevo     VARCHAR(20) NOT NULL,
    fecha_cambio     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_reserva FOREIGN KEY (id_reserva) REFERENCES reservas(id_reserva) ON DELETE CASCADE,
    CONSTRAINT fk_hist_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. PAGOS Y COMPROBANTES
--    El pago lo registra EXACTAMENTE un miembro del personal:
--    un agente o un administrador, nunca un cliente.
-- ---------------------------------------------------------------------
CREATE TABLE pagos (
    id_pago           INT AUTO_INCREMENT PRIMARY KEY,
    id_reserva        INT           NOT NULL,
    id_agente         INT           NULL,
    id_administrador  INT           NULL,
    monto             DECIMAL(10,2) NOT NULL,
    fecha             DATE          NOT NULL,
    metodo            ENUM('EFECTIVO','TRANSFERENCIA') NOT NULL,
    concepto          VARCHAR(150),
    CONSTRAINT fk_pago_reserva FOREIGN KEY (id_reserva)       REFERENCES reservas(id_reserva),
    CONSTRAINT fk_pago_agente  FOREIGN KEY (id_agente)        REFERENCES agentes(id_usuario),
    CONSTRAINT fk_pago_admin   FOREIGN KEY (id_administrador) REFERENCES administradores(id_usuario),
    CONSTRAINT ck_pago_monto   CHECK (monto > 0),
    CONSTRAINT ck_pago_quien   CHECK ((id_agente IS NULL) <> (id_administrador IS NULL))
) ENGINE=InnoDB;

CREATE TABLE comprobantes (
    id_comprobante      INT AUTO_INCREMENT PRIMARY KEY,
    id_pago             INT           NOT NULL UNIQUE,
    numero_correlativo  INT           NOT NULL UNIQUE,
    fecha_emision       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    importe             DECIMAL(10,2) NOT NULL,
    saldo_restante      DECIMAL(10,2) NOT NULL,
    concepto            VARCHAR(150),
    CONSTRAINT fk_comprobante_pago FOREIGN KEY (id_pago) REFERENCES pagos(id_pago)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. REPORTES (solo el administrador)
-- ---------------------------------------------------------------------
CREATE TABLE reportes (
    id_reporte        INT AUTO_INCREMENT PRIMARY KEY,
    id_administrador  INT          NOT NULL,
    tipo              VARCHAR(40)  NOT NULL,
    fecha_inicio      DATE         NOT NULL,
    fecha_fin         DATE         NOT NULL,
    fecha_generacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    formato           ENUM('PANTALLA','PDF','EXCEL') NOT NULL DEFAULT 'PANTALLA',
    CONSTRAINT fk_reporte_admin FOREIGN KEY (id_administrador) REFERENCES administradores(id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. ÍNDICES
-- ---------------------------------------------------------------------
CREATE INDEX idx_paquete_destino  ON paquetes_turisticos(id_destino);
CREATE INDEX idx_paquete_catalogo ON paquetes_turisticos(publicado, activo);
CREATE INDEX idx_vuelo_ruta       ON vuelos(origen, destino, fecha);
CREATE INDEX idx_reserva_cliente  ON reservas(id_cliente);
CREATE INDEX idx_pago_reserva     ON pagos(id_reserva);

-- =====================================================================
-- DATOS DE PRUEBA
--   admin@travelzen.com   -> Admin123*
--   agente@travelzen.com  -> Agente123*
--   cliente@travelzen.com -> Cliente123*
-- =====================================================================
INSERT INTO roles (nombre, descripcion) VALUES
 ('ADMIN',   'Administra usuarios, roles y reportes'),
 ('AGENTE',  'Gestiona clientes, vuelos, paquetes, reservas y pagos'),
 ('CLIENTE', 'Consulta catálogo, reserva y revisa su historial');

INSERT INTO usuarios (correo, contrasena_cifrada, id_rol) VALUES
 ('admin@travelzen.com',
  '$2a$10$VxY1ZVfaPjeDC2JNOxJmZOJIgIjajaaH3Uo/wh.791eD65wfqEKey', 1),
 ('agente@travelzen.com',
  '$2a$10$8tz6DJPgsjykqlXbYSzqjut8mPxrRL576XiGNMF617h7Wm8HDc0Gq', 2),
 ('cliente@travelzen.com',
  '$2a$10$EYtHWJ5/76852QbL1K/gduoUQCUkULyhjb2G4kLMAigYMTU4IRToG', 3);

INSERT INTO administradores
 (id_usuario, nombre_completo, documento_identidad, telefono, sexo, nacionalidad, fecha_nacimiento, fecha_ingreso) VALUES
 (1, 'Administrador General', '0000001 LP', '70000001', 'M', 'Boliviana', '1985-01-15', CURDATE());

INSERT INTO agentes
 (id_usuario, nombre_completo, documento_identidad, telefono, sexo, nacionalidad, fecha_nacimiento, fecha_ingreso) VALUES
 (2, 'Agente de Prueba', '0000002 LP', '70000002', 'F', 'Boliviana', '1990-05-20', CURDATE());

INSERT INTO clientes
 (nombre_completo, documento_identidad, correo, telefono, sexo, nacionalidad, fecha_nacimiento, id_usuario) VALUES
 ('Cliente de Prueba', '1234567 LP', 'cliente@travelzen.com', '70000000', 'M', 'Boliviana', '1995-03-10', 3),
 -- Cliente de mostrador SIN cuenta: sirve para probar "vincular cuenta" al registrarse
 ('Juan Pérez',        '7654321 LP', 'juan@gmail.com',        '71111111', 'M', 'Boliviana', '1988-09-02', NULL);

INSERT INTO destinos (nombre, pais, descripcion) VALUES
 ('Cancún',       'México',    'Playas del Caribe mexicano'),
 ('Cusco',        'Perú',      'Capital histórica del Imperio Inca'),
 ('Buenos Aires', 'Argentina', 'Capital cultural del Río de la Plata');

INSERT INTO paquetes_turisticos
 (nombre, id_destino, duracion_dias, descripcion, servicios_incluidos, precio, cupos_disponibles, imagen_portada, publicado) VALUES
 ('Caribe Total',         1, 7, 'Siete días frente al mar', 'Vuelo, hotel 4*, traslados, desayuno', 1850.00, 20, 'assets/img/cancun.jpg', TRUE),
 ('Cusco y Machu Picchu', 2, 5, 'Ruta clásica inca',        'Hotel, tren, entradas, guía',           980.00, 15, 'assets/img/cusco.jpg',  TRUE),
 ('Tango y Ciudad',       3, 4, 'Escapada urbana',          'Hotel, city tour',                      720.00, 10, 'assets/img/buenosaires.jpg', FALSE);
