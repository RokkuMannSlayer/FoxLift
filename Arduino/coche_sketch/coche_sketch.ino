#include <SoftwareSerial.h>
#include <Wire.h>
#include <Adafruit_PWMServoDriver.h>

// --- BLUETOOTH (Pin 2 RX, Pin 3 TX) ---
SoftwareSerial bluetooth(2, 3); 

// --- PCA9685 (Brazo Robótico) ---
Adafruit_PWMServoDriver pca = Adafruit_PWMServoDriver();
#define SERVOMIN  150 
#define SERVOMAX  600 

// Asignación de canales en el PCA9685
const int SERVO_BASE    = 0; // MG90S
const int SERVO_HOMBRO  = 1; // MG90S
const int SERVO_CODO    = 2; // SG90
const int SERVO_MUNICA  = 3; // SG90
const int SERVO_PINZA   = 4; // SG90

// --- DRIVERS L298N ---
const int IN1 = 4; // Ruedas Izquierdas
const int IN2 = 5;
const int IN3 = 6; // Ruedas Derechas
const int IN4 = 7;

// --- SENSORES DE ULTRASONIDO ---
const int TRIG_BUSQUEDA = 8;  // Sensor que escanea el entorno
const int ECHO_BUSQUEDA = 9;
const int TRIG_REGISTRO = 10; // Sensor a corta distancia para confirmación/registro
const int ECHO_REGISTRO = 11;

// --- VARIABLES DE CONTROL ---
bool modoAutonomo = false;
int posBaseActual = 90; // Rastreo del ángulo de la base

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);   
  
  pinMode(IN1, OUTPUT); pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT); pinMode(IN4, OUTPUT);
  
  pinMode(TRIG_BUSQUEDA, OUTPUT); pinMode(ECHO_BUSQUEDA, INPUT);
  pinMode(TRIG_REGISTRO, OUTPUT); pinMode(ECHO_REGISTRO, INPUT);
  
  pca.begin();
  pca.setPWMFreq(60); 
  
  // Posición de reposo o inicial del brazo (Segura para no forzar la estructura)
  moverServo(SERVO_BASE, 90);
  moverServo(SERVO_HOMBRO, 45); // Ligeramente retraído
  moverServo(SERVO_CODO, 120);
  moverServo(SERVO_MUNICA, 90);
  moverServo(SERVO_PINZA, 0);   // Pinza abierta

  Serial.println("--- Sistema de Búsqueda y Registro Listo ---");
}

void loop() {
  // 1. Lectura de sensores
  long distBusqueda = obtenerDistancia(TRIG_BUSQUEDA, ECHO_BUSQUEDA);
  long distRegistro = obtenerDistancia(TRIG_REGISTRO, ECHO_REGISTRO);

  // 2. Lógica Autónoma si está activa
  if (modoAutonomo) {
    ejecutarRastreo(distBusqueda, distRegistro);
  }

  // 3. Control por Bluetooth (Manual e interrupción)
  if (bluetooth.available()) {
    char comando = bluetooth.read();
    
    switch (comando) {
      case 'A': // Activar modo Autónomo de Búsqueda
        modoAutonomo = true;
        Serial.println("Modo Autónomo: ACTIVADO");
        break;
      case 'M': // Volver a modo Manual
        modoAutonomo = false;
        detenerMotores();
        Serial.println("Modo Autónomo: DESACTIVADO (Manual)");
        break;
        
      // Controles manuales de movimiento (Solo operan en modo manual)
      case 'F': if(!modoAutonomo) avanzar(); break;
      case 'B': if(!modoAutonomo) retroceder(); break;
      case 'L': if(!modoAutonomo) girarIzquierda(); break;
      case 'R': if(!modoAutonomo) girarDerecha(); break;
      case 'S': detenerMotores(); break;

      // Comandos manuales para la Pinza/Brazo (Ejemplo)
      case 'O': moverServo(SERVO_PINZA, 0);   break; // Abrir pinza
      case 'C': moverServo(SERVO_PINZA, 90);  break; // Cerrar pinza
    }
  }
}

// --- LÓGICA DE BÚSQUEDA Y REGISTRO MEJORADA (Sensor fijo en chasis) ---
void ejecutarRastreo(long busqueda, long registro) {
  
  // FASE 3: CONFIRMACIÓN Y REGISTRO (El objeto está en zona de agarre)
  if (registro > 0 && registro <= 6) {
    detenerMotores();
    Serial.println("[REGISTRO] ¡Objetivo alineado y en posición! Iniciando rutina del brazo...");
    bluetooth.println("REGISTRANDO_OBJETO"); 
    
    // --- SECUENCIA DE AGARRE DEL BRAZO ROBÓTICO ---
    // Paso 1: Asegurar que la pinza esté completamente abierta y la base centrada
    moverServo(SERVO_PINZA, 0);     // Abre pinza (SG90)
    moverServo(SERVO_BASE, 90);     // Centra la base hacia el frente (MG90S)
    delay(600);

    // Paso 2: Estirar el brazo hacia el frente y abajo (Hombro y Codo bajan)
    moverServo(SERVO_CODO, 150);    // Ajusta el ángulo del codo (SG90)
    moverServo(SERVO_HOMBRO, 120);  // Baja el hombro hacia el objeto (MG90S)
    moverServo(SERVO_MUNICA, 120);  // Inclina la muñeca para aproximar la pinza
    delay(800);                     // Espera a que los motores completen el recorrido

    // Paso 3: Cerrar la pinza para sujetar el objeto
    moverServo(SERVO_PINZA, 85);    // Cierra la pinza (Ajusta según tamaño del objeto)
    delay(600);

    // Paso 4: Levantar el objeto (El Hombro sube primero para hacer fuerza)
    moverServo(SERVO_HOMBRO, 45);   // Levanta el hombro (MG90S)
    delay(500);
    moverServo(SERVO_CODO, 90);     // Retrae el codo a posición segura (SG90)
    moverServo(SERVO_MUNICA, 90);   // Modera la muñeca
    delay(600);

    // Paso 5: Depositar o Registrar (Girar base a la izquierda a un contenedor trasero)
    moverServo(SERVO_BASE, 180);    // Gira la base totalmente a un costado o atrás
    delay(800);
    moverServo(SERVO_PINZA, 0);     // Abre la pinza para soltar el objeto registrado
    delay(500);

    // Paso 6: Regresar el brazo a la postura de descanso inicial
    moverServo(SERVO_BASE, 90);
    moverServo(SERVO_HOMBRO, 45);
    moverServo(SERVO_CODO, 120);
    delay(600);

    Serial.println("[REGISTRO] Operación completada con éxito.");
    bluetooth.println("LISTO_PARA_NUEVO_OBJETO");
    
    modoAutonomo = false; // Finaliza la automatización y espera órdenes por Bluetooth
    return;
  }

  // FASE 2: APROXIMACIÓN (El chasis detecta el objeto en frente, pero lejos)
  if (busqueda > 6 && busqueda <= 40) {
    Serial.print("[BÚSQUEDA] Objeto detectado en trayectoria a: "); 
    Serial.print(busqueda); Serial.println(" cm. Avanzando recto...");
    avanzar(); // Va de frente hacia el objeto
  } 
  
  // FASE 1: BÚSQUEDA/ESCANEO (El sensor fijo no ve nada adelante)
  else {
    Serial.println("[BÚSQUEDA] Escaneando entorno (Giro del chasis)...");
    girarDerecha(); // El coche gira en su propio eje buscando el objetivo
    delay(80);      // Pulsos muy cortos de giro para no pasarse del objeto
    detenerMotores();
    delay(150);     // Pausa para estabilizar la lectura del sensor ultrasónico
  }
}


// --- FUNCIONES DE MOVIMIENTO MECÁNICO ---
void moverServo(int numeroServo, int grados) {
  int pulso = map(grados, 0, 180, SERVOMIN, SERVOMAX);
  pca.setPWM(numeroServo, 0, pulso);
}

void avanzar() {
  digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
}

void retroceder() {
  digitalWrite(IN1, LOW);  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW);  digitalWrite(IN4, HIGH);
}

void girarIzquierda() {
  digitalWrite(IN1, LOW);  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
}

void girarDerecha() {
  digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);  digitalWrite(IN4, HIGH);
}

void detenerMotores() {
  digitalWrite(IN1, LOW); digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW); digitalWrite(IN4, LOW);
}

long obtenerDistancia(int pinTrig, int pinEcho) {
  digitalWrite(pinTrig, LOW); delayMicroseconds(2);
  digitalWrite(pinTrig, HIGH); delayMicroseconds(10);
  digitalWrite(pinTrig, LOW);
  long duracion = pulseIn(pinEcho, HIGH, 25000); 
  return duracion * 0.034 / 2;
}
