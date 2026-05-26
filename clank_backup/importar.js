const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const ProgressBar = require('progress');

const CREDENTIALS_PATH = './serviceAccountKey.json';
const DATA_PATH = './categorias.json';

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function ejecutarImportacion() {
  process.stdout.write('\x1Bc');
  console.log('\nIniciando proceso de replicación de la BBDD...');

  if (!fs.existsSync(CREDENTIALS_PATH)) {
    console.error('\nERROR: No se encuentra el archivo "serviceAccountKey.json".');
    console.log('Debes descargarlo desde la Consola de Firebase y ponerlo en esta carpeta.\n');
    process.exit(1);
  }

  if (!fs.existsSync(DATA_PATH)) {
    console.error('\nERROR: No se encuentra el archivo "categorias.json".');
    console.log('Asegúrate de haber ejecutado primero el script de extracción.\n');
    process.exit(1);
  }

  try {
    const serviceAccount = require(CREDENTIALS_PATH);
    const datos = JSON.parse(fs.readFileSync(DATA_PATH, 'utf8'));

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });

    const db = admin.firestore();
    const batch = db.batch();
    const nombreColeccion = 'categorias';

    console.log(`\nPreparando ${datos.length} documentos para importar...`);

    const bar = new ProgressBar('Progreso [:bar] :percent :etas', {
      complete: '=',
      incomplete: ' ',
      width: 30,
      total: datos.length
    });

    datos.forEach((doc) => {
      const { id, ...contenido } = doc;
      const docRef = db.collection(nombreColeccion).doc(id);
      batch.set(docRef, contenido);

      bar.tick();
    });

    await batch.commit();

    console.log('\n¡¡¡¡ATENCIÓN!!!!');
    await sleep(1500);
    console.log('\nTu equipo se autodestruirá en...');
    await sleep(2000);
    for (let i = 5; i > 0; i--) {
      await sleep(1500);
      console.log(`   ${i}...`);
    }

    await sleep(1000);
    process.stdout.write('\x1Bc'); 

    console.log('\nEs bromaa');
    console.log('\nParece que nada ha explotado. Andrea y Lore han conseguido guiarte bien hasta aqui!');
	
console.log('\n');
console.log('\n');



    console.log(`
 ██████╗██╗      █████╗ ███╗   ██╗██╗  ██╗
██╔════╝██║     ██╔══██╗████╗  ██║██║ ██╔╝
██║     ██║     ███████║██╔██╗ ██║█████╔╝
██║     ██║     ██╔══██║██║╚██╗██║██╔═██╗
╚██████╗███████╗██║  ██║██║ ╚████║██║  ██╗
 ╚═════╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝
    `);

    console.log(`Estructura replicada con éxito.`);
    console.log(`${datos.length} documentos cargados en la colección "${nombreColeccion}".\n`);

  } catch (error) {
    console.error('\nError durante la importación:');
    console.error(error.message);
    console.log('Revisa que tus credenciales tengan permisos de Editor o Propietario.\n');
  }
}

ejecutarImportacion();
