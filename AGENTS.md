# Instrucciones de trabajo de SVU

SVU gestiona PQRSD de la Institución Universitaria del Putumayo. Comunicarse en español. El usuario coordina la implementación local con la planificación y revisión independiente de ChatGPT en GitHub.

## Alcance y control de cambios

- Para el issue #152, trabajar exclusivamente en `feat/informes-gestion-pqrsd` del repositorio `CusmaLinux/svu`. Verificar la rama y el estado de Git al iniciar. Mantener `main` fuera de los cambios.
- Respetar el alcance autorizado en cada encargo. El [plan del issue](docs/ISSUE-152-PLAN.md) describe trabajo futuro y cada bloque requiere la autorización correspondiente.
- Obtener autorización expresa antes de hacer commits, push, merge o despliegues. Mantener toda intervención fuera de servidores y bases de datos de producción.
- Preservar los cambios locales del usuario, incluidos los de `package-lock.json`. Revisar los archivos existentes antes de editarlos. Evitar eliminaciones, reversiones y cambios incidentales.

## Implementación y verificación

- Mantener la compatibilidad de las funcionalidades actuales, especialmente la radicación y el seguimiento públicos. Revisar los consumidores Vue al cambiar contratos REST.
- Verificar los hallazgos en el código antes de corregirlos. Documentar la evidencia y las decisiones técnicas. Usar la [auditoría del bloque 1B](docs/SECURITY-AUDIT.md) para registrar los controles pendientes.
- Usar el Maven Wrapper del proyecto y las dependencias existentes. Incorporar dependencias solo con una necesidad justificada.
- Ejecutar pruebas acordes con la entrega antes de declararla terminada. Para integración, usar MongoDB aislado con Testcontainers y Docker. Emplear datos sintéticos y proteger los datos personales, tokens y credenciales en pruebas, trazas, documentación e informes.
- Distinguir dato faltante, indicador no calculable y cero observado. Documentar las reglas de cálculo y aplicar la misma interpretación en API, previsualización y exportaciones.
