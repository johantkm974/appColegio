DROP DATABASE IF EXISTS reporta_db;
CREATE DATABASE reporta_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE reporta_db;

-- ============================================================
-- 1. ROLES Y USUARIOS
-- ============================================================

CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE, -- ADMIN, ENCARGADO, DOCENTE
    descripcion VARCHAR(150) NULL
);

CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rol_id INT NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    dni VARCHAR(15) NOT NULL UNIQUE,
    correo VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    telefono VARCHAR(20) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles(id)
);

-- ============================================================
-- 2. INFRAESTRUCTURA (CRUD EXCLUSIVO DEL ROL ADMIN)
-- ============================================================

CREATE TABLE edificios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE, -- ED-04 .. ED-10
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE pisos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    edificio_id INT NOT NULL,
    numero_piso TINYINT NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_edificio_piso (edificio_id, numero_piso),
    CONSTRAINT fk_pisos_edificio FOREIGN KEY (edificio_id) REFERENCES edificios(id)
);

CREATE TABLE ambientes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    piso_id INT NOT NULL,
    tipo VARCHAR(40) NOT NULL,      -- AULA, TALLER, INVESTIGACION, LABORATORIO, AIP, BANO, SALA, ADMIN, DEPOSITO, BIBLIOTECA, TOPICO, MAESTRANZA, CONECTIVIDAD
    especialidad VARCHAR(100) NULL, -- Metales, Ebanistería, Vestidos, Electrónica, Primaria, Secundaria, Flex, CyT, Alumnos Hombres, etc.
    codigo VARCHAR(20) NULL,        -- Número real de aula ('1'..'30'), sufijo de taller ('T1','T2') o NULL si está pendiente
    nombre VARCHAR(120) NOT NULL,   -- Etiqueta visible en la app
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ambientes_piso_tipo (piso_id, tipo),
    INDEX idx_ambientes_codigo (codigo),
    CONSTRAINT fk_ambientes_piso FOREIGN KEY (piso_id) REFERENCES pisos(id)
);

-- ============================================================
-- 3. REPORTES DE DAÑOS E HISTORIAL DE ESTADOS
-- ============================================================

CREATE TABLE reportes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_ticket VARCHAR(25) NOT NULL UNIQUE, -- Ej: REP-2026-0001
    ambiente_id INT NOT NULL,
    docente_id BIGINT NOT NULL,                -- Docente que registra el daño
    encargado_id BIGINT NULL,                  -- Encargado asignado que atiende el reporte
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    foto_url VARCHAR(500) NULL,
    prioridad ENUM('BAJA', 'MEDIA', 'ALTA', 'URGENTE') NOT NULL DEFAULT 'MEDIA',
    estado ENUM('POR_ATENDER', 'EN_PROCESO', 'ATENDIDO', 'RECHAZADO') NOT NULL DEFAULT 'POR_ATENDER',
    observacion_atencion TEXT NULL,
    fecha_reporte TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    fecha_atencion TIMESTAMP NULL,
    INDEX idx_reportes_estado (estado),
    INDEX idx_reportes_docente (docente_id),
    INDEX idx_reportes_encargado (encargado_id),
    INDEX idx_reportes_ambiente (ambiente_id),
    CONSTRAINT fk_reportes_ambiente FOREIGN KEY (ambiente_id) REFERENCES ambientes(id),
    CONSTRAINT fk_reportes_docente FOREIGN KEY (docente_id) REFERENCES usuarios(id),
    CONSTRAINT fk_reportes_encargado FOREIGN KEY (encargado_id) REFERENCES usuarios(id)
);

CREATE TABLE historial_reportes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporte_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    estado_anterior VARCHAR(30) NULL,
    estado_nuevo VARCHAR(30) NOT NULL,
    comentario VARCHAR(255) NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_reporte FOREIGN KEY (reporte_id) REFERENCES reportes(id) ON DELETE CASCADE,
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- ============================================================
-- 4. DATOS SEMILLA: ROLES Y USUARIOS BASE
-- ============================================================

INSERT INTO roles (id, nombre, descripcion) VALUES
(1, 'ADMIN', 'Administra edificios, pisos, ambientes, usuarios y supervisa reportes'),
(2, 'ENCARGADO', 'Atiende los reportes de daños y actualiza estados'),
(3, 'DOCENTE', 'Registra reportes de daños y consulta el estado de sus solicitudes');

-- Reemplazar los password_hash por hashes BCrypt reales generados en tu backend
INSERT INTO usuarios (rol_id, nombres, apellidos, dni, correo, password_hash, telefono) VALUES
(1, 'Johan', 'Vasquez', '00000001', 'admin@institucion.edu.pe', '$2a$10$hashDePruebaAdmin', '999000111'),
(2, 'Carlos', 'Mendoza', '00000002', 'encargado@institucion.edu.pe', '$2a$10$hashDePruebaEncargado', '999000222'),
(3, 'Maria', 'Torres', '00000003', 'docente@institucion.edu.pe', '$2a$10$hashDePruebaDocente', '999000333');

-- ============================================================
-- 5. DATOS SEMILLA: EDIFICIOS (04 AL 10) Y PISOS (23 PISOS)
-- ============================================================

INSERT INTO edificios (id, codigo, nombre, descripcion) VALUES
(1, 'ED-04', 'Edificio 04', 'Talleres de ebanistería, vestidos e investigación'),
(2, 'ED-05', 'Edificio 05', 'Talleres de metales, electrónica, artesanía y almacén general'),
(3, 'ED-06', 'Edificio 06', 'Aulas de Primaria, AIP Primaria, Taller de Arte y Taller Creativo'),
(4, 'ED-07', 'Edificio 07', 'Aulas de Primaria y Secundaria, AIP, Biblioteca y Taller de Arte'),
(5, 'ED-08', 'Edificio 08', 'Aulas de Secundaria, Aulas Flex, AIP, Artesanía y Conectividad'),
(6, 'ED-09', 'Edificio 09', 'Administración, Salas de Docentes, Laboratorios CyT y Aulas de Secundaria'),
(7, 'ED-10', 'Edificio 10', 'Aulas de Secundaria, Aulas Flex, Taller de Arte y Depósito Deportivo');

INSERT INTO pisos (id, edificio_id, numero_piso, nombre) VALUES
-- Edificio 04 (3 pisos)
(1, 1, 1, 'Piso 1'), (2, 1, 2, 'Piso 2'), (3, 1, 3, 'Piso 3'),
-- Edificio 05 (2 pisos)
(4, 2, 1, 'Piso 1'), (5, 2, 2, 'Piso 2'),
-- Edificio 06 (4 pisos)
(6, 3, 1, 'Piso 1'), (7, 3, 2, 'Piso 2'), (8, 3, 3, 'Piso 3'), (9, 3, 4, 'Piso 4'),
-- Edificio 07 (3 pisos)
(10, 4, 1, 'Piso 1'), (11, 4, 2, 'Piso 2'), (12, 4, 3, 'Piso 3'),
-- Edificio 08 (4 pisos)
(13, 5, 1, 'Piso 1'), (14, 5, 2, 'Piso 2'), (15, 5, 3, 'Piso 3'), (16, 5, 4, 'Piso 4'),
-- Edificio 09 (4 pisos)
(17, 6, 1, 'Piso 1'), (18, 6, 2, 'Piso 2'), (19, 6, 3, 'Piso 3'), (20, 6, 4, 'Piso 4'),
-- Edificio 10 (3 pisos)
(21, 7, 1, 'Piso 1'), (22, 7, 2, 'Piso 2'), (23, 7, 3, 'Piso 3');

-- ============================================================
-- 6. DATOS SEMILLA: AMBIENTES POR EDIFICIO Y PISO
-- ============================================================

-- ------------------------------------------------------------
-- EDIFICIO 04 (3 pisos: Ebanistería y Vestidos)
-- ------------------------------------------------------------
INSERT INTO ambientes (piso_id, tipo, especialidad, codigo, nombre) VALUES
-- Piso 1 (piso_id = 1)
(1, 'TALLER', 'Ebanistería', 'T1', 'Taller de Ebanistería T1'),
(1, 'INVESTIGACION', 'Ebanistería', NULL, 'Investigación de Ebanistería'),
(1, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(1, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(1, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 2 (piso_id = 2)
(2, 'TALLER', 'Vestidos', 'T1', 'Taller de Vestidos T1'),
(2, 'INVESTIGACION', 'Vestidos', NULL, 'Investigación de Vestidos'),
(2, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(2, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(2, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 3 (piso_id = 3)
(3, 'TALLER', 'Vestidos', 'T2', 'Taller de Vestidos T2'),
(3, 'INVESTIGACION', 'Vestidos', NULL, 'Investigación de Vestidos'),
(3, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(3, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(3, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible');

-- ------------------------------------------------------------
-- EDIFICIO 05 (2 pisos: Metales, Electrónica, Artesanía y Almacén)
-- ------------------------------------------------------------
INSERT INTO ambientes (piso_id, tipo, especialidad, codigo, nombre) VALUES
-- Piso 1 (piso_id = 4)
(4, 'TALLER', 'Metales', 'T1', 'Taller de Metales T1'),
(4, 'TALLER', 'Metales', 'T2', 'Taller de Metales T2'),
(4, 'INVESTIGACION', 'Metales', NULL, 'Investigación de Metales'),
(4, 'TALLER', 'Artesanía', 'T1', 'Taller de Artesanía T1'),
(4, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(4, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(4, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 2 (piso_id = 5)
(5, 'TALLER', 'Electrónica', 'T1', 'Taller de Electrónica T1'),
(5, 'INVESTIGACION', 'Electrónica', NULL, 'Investigación de Electrónica'),
(5, 'TALLER', 'Artesanía', 'T1', 'Taller de Artesanía T1'),
(5, 'DEPOSITO', 'Almacén General', NULL, 'Almacén General'),
(5, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(5, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(5, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible');

-- ------------------------------------------------------------
-- EDIFICIO 06 (4 pisos: Primaria, AIP, Arte y Creativo)
-- ------------------------------------------------------------
INSERT INTO ambientes (piso_id, tipo, especialidad, codigo, nombre) VALUES
-- Piso 1 (piso_id = 6)
(6, 'AIP', 'Primaria', NULL, 'AIP Primaria'),
(6, 'TALLER', 'Arte', 'T1', 'Taller de Arte T1'),
(6, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 1)'),
(6, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 2)'),
(6, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 3)'),
(6, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(6, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(6, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 2 (piso_id = 7)
(7, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 1)'),
(7, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 2)'),
(7, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 3)'),
(7, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 4)'),
(7, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 5)'),
(7, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(7, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(7, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 3 (piso_id = 8)
(8, 'TALLER', 'Creativo', 'T1', 'Taller Creativo T1'),
(8, 'TALLER', 'Creativo', 'T2', 'Taller Creativo T2'),
(8, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 1)'),
(8, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 2)'),
(8, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(8, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(8, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 4 (piso_id = 9)
(9, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 1)'),
(9, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 2)'),
(9, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 3)'),
(9, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 4)'),
(9, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 5)'),
(9, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(9, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(9, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible');

-- ------------------------------------------------------------
-- EDIFICIO 07 (3 pisos)
-- ------------------------------------------------------------
INSERT INTO ambientes (piso_id, tipo, especialidad, codigo, nombre) VALUES
-- Piso 1 (piso_id = 10)
(10, 'AIP', 'Secundaria', NULL, 'AIP Secundaria'),
(10, 'BIBLIOTECA', 'General', NULL, 'Biblioteca'),
(10, 'AULA', 'Secundaria', NULL, 'Aula Secundaria (Sin código 1)'),
(10, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(10, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(10, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 2 (piso_id = 11)
(11, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 1)'),
(11, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 2)'),
(11, 'AULA', 'Primaria', NULL, 'Aula Primaria (Sin código 3)'),
(11, 'AIP', 'Primaria', NULL, 'AIP Primaria'),
(11, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(11, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(11, 'BANO', 'Accesible', NULL, 'Baño Accesible'),
-- Piso 3 (piso_id = 12)
(12, 'AULA', 'Secundaria', '28', 'Aula Secundaria 28'),
(12, 'AULA', 'Secundaria', '29', 'Aula Secundaria 29'),
(12, 'AULA', 'Secundaria', '30', 'Aula Secundaria 30'),
(12, 'TALLER', 'Arte', 'T1', 'Taller de Arte T1'),
(12, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(12, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(12, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible');

-- ------------------------------------------------------------
-- EDIFICIO 08 (4 pisos)
-- ------------------------------------------------------------
INSERT INTO ambientes (piso_id, tipo, especialidad, codigo, nombre) VALUES
-- Piso 1 (piso_id = 13)
(13, 'AULA', 'Secundaria', '17', 'Aula Secundaria 17'),
(13, 'AULA', 'Secundaria', '18', 'Aula Secundaria 18'),
(13, 'AIP', 'Secundaria', NULL, 'AIP Secundaria'),
(13, 'TALLER', 'Artesanía', 'T1', 'Taller de Artesanía T1'),
(13, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(13, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(13, 'BANO', 'Accesible', NULL, 'Baño Accesible'),
-- Piso 2 (piso_id = 14)
(14, 'AULA', 'Flex', NULL, 'Aula Flex (Sin código 1)'),
(14, 'AULA', 'Flex', NULL, 'Aula Flex (Sin código 2)'),
(14, 'CONECTIVIDAD', 'Módulo', NULL, 'Módulo de Conectividad'),
(14, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(14, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(14, 'BANO', 'Accesible', NULL, 'Baño Accesible'),
-- Piso 3 (piso_id = 15)
(15, 'AULA', 'Secundaria', '19', 'Aula Secundaria 19'),
(15, 'AULA', 'Secundaria', '20', 'Aula Secundaria 20'),
(15, 'AULA', 'Secundaria', '21', 'Aula Secundaria 21'),
(15, 'AIP', 'Secundaria', NULL, 'AIP Secundaria'),
(15, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(15, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(15, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 4 (piso_id = 16)
(16, 'AULA', 'Secundaria', '22', 'Aula Secundaria 22'),
(16, 'AULA', 'Secundaria', '23', 'Aula Secundaria 23'),
(16, 'AULA', 'Secundaria', '24', 'Aula Secundaria 24'),
(16, 'AIP', 'Secundaria', NULL, 'AIP Secundaria'),
(16, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(16, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(16, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible');

-- ------------------------------------------------------------
-- EDIFICIO 09 (4 pisos)
-- ------------------------------------------------------------
INSERT INTO ambientes (piso_id, tipo, especialidad, codigo, nombre) VALUES
-- Piso 1 (piso_id = 17 - Administrativo)
(17, 'SALA', 'Docentes Secundaria', NULL, 'Sala de Docentes Secundaria'),
(17, 'SALA', 'Docentes Primaria', NULL, 'Sala de Docentes Primaria'),
(17, 'TOPICO', 'Salud', NULL, 'Tópico'),
(17, 'MAESTRANZA', 'Mantenimiento', NULL, 'Maestranza'),
(17, 'DEPOSITO', 'Almacén General', NULL, 'Almacén General'),
(17, 'ADMIN', 'Administración', NULL, 'Administración'),
(17, 'BANO', 'ADM Hombres / Accesible', NULL, 'Baño Administrativo Hombres (Accesible)'),
(17, 'BANO', 'ADM Mujeres / Accesible', NULL, 'Baño Administrativo Mujeres (Accesible)'),
-- Piso 2 (piso_id = 18)
(18, 'LABORATORIO', 'CyT', NULL, 'Laboratorio CyT'),
(18, 'AULA', 'Secundaria', '8', 'Aula Secundaria 8'),
(18, 'AULA', 'Secundaria', '9', 'Aula Secundaria 9'),
(18, 'AULA', 'Secundaria', '10', 'Aula Secundaria 10'),
(18, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(18, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(18, 'BANO', 'Accesible', NULL, 'Baño Accesible'),
-- Piso 3 (piso_id = 19)
(19, 'LABORATORIO', 'CyT', NULL, 'Laboratorio CyT'),
(19, 'AULA', 'Secundaria', '11', 'Aula Secundaria 11'),
(19, 'AULA', 'Secundaria', '12', 'Aula Secundaria 12'),
(19, 'AULA', 'Secundaria', '13', 'Aula Secundaria 13'),
(19, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(19, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(19, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 4 (piso_id = 20)
(20, 'LABORATORIO', 'CyT', NULL, 'Laboratorio CyT'),
(20, 'AULA', 'Secundaria', '14', 'Aula Secundaria 14'),
(20, 'AULA', 'Secundaria', '15', 'Aula Secundaria 15'),
(20, 'AULA', 'Secundaria', '16', 'Aula Secundaria 16'),
(20, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(20, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(20, 'BANO', 'Accesible', NULL, 'Baño Accesible');

-- ------------------------------------------------------------
-- EDIFICIO 10 (3 pisos)
-- ------------------------------------------------------------
INSERT INTO ambientes (piso_id, tipo, especialidad, codigo, nombre) VALUES
-- Piso 1 (piso_id = 21)
(21, 'AULA', 'Secundaria', '1', 'Aula Secundaria 1'),
(21, 'AULA', 'Secundaria', '2', 'Aula Secundaria 2'),
(21, 'AULA', 'Secundaria', '3', 'Aula Secundaria 3'),
(21, 'AULA', 'Secundaria', '4', 'Aula Secundaria 4'),
(21, 'DEPOSITO', 'Deportivo', NULL, 'Depósito Deportivo'),
(21, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(21, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(21, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible'),
-- Piso 2 (piso_id = 22)
(22, 'SALA', 'Docentes Secundaria', NULL, 'Sala de Docentes Secundaria'),
(22, 'AULA', 'Flex Secundaria', NULL, 'Aula Flex Secundaria'),
(22, 'AULA', 'Flex Trabajo por Proyectos', NULL, 'Aula Flex Trabajo por Proyectos'),
(22, 'AULA', 'Flex Trabajo en Grupo', NULL, 'Aula Flex Trabajo en Grupo (Sin código 1)'),
(22, 'AULA', 'Flex Trabajo en Grupo', NULL, 'Aula Flex Trabajo en Grupo (Sin código 2)'),
(22, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(22, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(22, 'BANO', 'Accesible', NULL, 'Baño Accesible'),
-- Piso 3 (piso_id = 23)
(23, 'AULA', 'Secundaria', '5', 'Aula Secundaria 5'),
(23, 'AULA', 'Secundaria', '6', 'Aula Secundaria 6'),
(23, 'AULA', 'Secundaria', '7', 'Aula Secundaria 7'),
(23, 'TALLER', 'Arte', 'T1', 'Taller de Arte T1'),
(23, 'BANO', 'Alumnos Hombres', NULL, 'Baño Alumnos - Hombres'),
(23, 'BANO', 'Alumnos Mujeres', NULL, 'Baño Alumnos - Mujeres'),
(23, 'BANO', 'Docentes / Accesible', NULL, 'Baño Docentes / Accesible');

-- ============================================================
-- 7. CONSULTA DE VERIFICACIÓN RÁPIDA (EJECUTAR TRAS LA CARGA)
-- ============================================================
SELECT
    e.codigo AS edificio,
    p.numero_piso AS piso,
    COUNT(a.id) AS total_ambientes,
    SUM(a.tipo = 'AULA') AS total_aulas,
    SUM(a.tipo = 'TALLER') AS total_talleres,
    SUM(a.tipo = 'BANO') AS total_banos
FROM edificios e
JOIN pisos p ON p.edificio_id = e.id
LEFT JOIN ambientes a ON a.piso_id = p.id
GROUP BY e.id, p.id
ORDER BY e.id, p.numero_piso;