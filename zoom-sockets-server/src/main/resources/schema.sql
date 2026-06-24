-- Habilitar claves foráneas
PRAGMA foreign_keys = ON;

-- 1. Tabla Usuarios
CREATE TABLE IF NOT EXISTS Usuarios (
    IdUsuario INTEGER PRIMARY KEY AUTOINCREMENT,
    Nombres VARCHAR(100) NOT NULL,
    Correo VARCHAR(100) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Rol VARCHAR(50) NOT NULL,
    Activo BOOLEAN DEFAULT 1
);

-- 2. Tabla Salas
CREATE TABLE IF NOT EXISTS Salas (
    IdSala INTEGER PRIMARY KEY AUTOINCREMENT,
    CodigoSala VARCHAR(50) UNIQUE NOT NULL,
    Nombre VARCHAR(100) NOT NULL,
    IdHost INTEGER NOT NULL,
    Estado VARCHAR(50) NOT NULL, -- 'Activa', 'Finalizada'
    FechaCreacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (IdHost) REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE
);

-- 3. Tabla ParticipantesSala
CREATE TABLE IF NOT EXISTS ParticipantesSala (
    IdParticipante INTEGER PRIMARY KEY AUTOINCREMENT,
    IdSala INTEGER NOT NULL,
    IdUsuario INTEGER NOT NULL,
    Estado VARCHAR(50) NOT NULL, -- 'Activo', 'Inactivo'
    FechaIngreso TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (IdSala) REFERENCES Salas(IdSala) ON DELETE CASCADE,
    FOREIGN KEY (IdUsuario) REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE
);

-- 4. Tabla Mensajes
CREATE TABLE IF NOT EXISTS Mensajes (
    IdMensaje INTEGER PRIMARY KEY AUTOINCREMENT,
    IdSala INTEGER NOT NULL,
    IdUsuario INTEGER NOT NULL,
    Contenido TEXT NOT NULL,
    FechaEnvio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (IdSala) REFERENCES Salas(IdSala) ON DELETE CASCADE,
    FOREIGN KEY (IdUsuario) REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE
);

-- 5. Tabla ArchivosCompartidos
CREATE TABLE IF NOT EXISTS ArchivosCompartidos (
    IdArchivo INTEGER PRIMARY KEY AUTOINCREMENT,
    IdSala INTEGER NOT NULL,
    IdUsuario INTEGER NOT NULL,
    NombreArchivo VARCHAR(255) NOT NULL,
    RutaArchivo VARCHAR(512) NOT NULL,
    FechaEnvio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (IdSala) REFERENCES Salas(IdSala) ON DELETE CASCADE,
    FOREIGN KEY (IdUsuario) REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE
);

-- 6. Tabla SolicitudesSala
CREATE TABLE IF NOT EXISTS SolicitudesSala (
    IdSolicitud INTEGER PRIMARY KEY AUTOINCREMENT,
    IdSala INTEGER NOT NULL,
    IdUsuario INTEGER NOT NULL,
    Estado VARCHAR(50) NOT NULL, -- 'Pendiente', 'Aceptada', 'Rechazada'
    FechaSolicitud TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (IdSala) REFERENCES Salas(IdSala) ON DELETE CASCADE,
    FOREIGN KEY (IdUsuario) REFERENCES Usuarios(IdUsuario) ON DELETE CASCADE
);

-- Insertar datos semilla (Contraseña por defecto para todos: "password123", hash generado con BCrypt)
-- Hash: $2a$10$tMh4zHl39Vd1vB7dO/wzquPkyTj1Wc5V1rF14qA2l/jN1T9gEqvjS
INSERT OR IGNORE INTO Usuarios (Nombres, Correo, PasswordHash, Rol, Activo) VALUES
('Host Demo', 'host@zoom.com', '$2a$10$tMh4zHl39Vd1vB7dO/wzquPkyTj1Wc5V1rF14qA2l/jN1T9gEqvjS', 'Docente', 1),
('Invitado Juan', 'juan@zoom.com', '$2a$10$tMh4zHl39Vd1vB7dO/wzquPkyTj1Wc5V1rF14qA2l/jN1T9gEqvjS', 'Estudiante', 1),
('Invitada Maria', 'maria@zoom.com', '$2a$10$tMh4zHl39Vd1vB7dO/wzquPkyTj1Wc5V1rF14qA2l/jN1T9gEqvjS', 'Estudiante', 1);
