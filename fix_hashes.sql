UPDATE usuarios SET password_hash = '$2b$10$1oqiujl7dwzE/3zaZdBqDOcBcafLugp9rJYRpvDIkXTdBiqKA1sse' WHERE correo = 'admin@institucion.edu.pe';
UPDATE usuarios SET password_hash = '$2b$10$HjqrT3B8FL/iSbDdICCZf.y5IrKDvt5BQZ.gvQ2SMcazegeIt08ze' WHERE correo = 'encargado@institucion.edu.pe';
UPDATE usuarios SET password_hash = '$2b$10$YVrlj78hQoO//BjX1wSpL.KA3LOBp7BeTE6PuudEYOvI1Qw8PgX1i' WHERE correo = 'docente@institucion.edu.pe';
SELECT id, correo, LEFT(password_hash, 30) AS hash_inicio FROM usuarios;
