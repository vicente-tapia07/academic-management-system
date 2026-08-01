const replicaSetName = process.env.MONGO_REPLICA_SET || "rs0";

const replicaSetConfig = {
  _id: replicaSetName,
  members: [
    { _id: 0, host: "mongo-primary:27017", priority: 2 },
    { _id: 1, host: "mongo-secondary:27017", priority: 1 }
  ]
};

try {
  const currentStatus = rs.status();
  if (currentStatus.set !== replicaSetName) {
    throw new Error(
      `El Replica Set existente se llama ${currentStatus.set}, no ${replicaSetName}`
    );
  }
  print(`Replica Set ${replicaSetName} ya estaba inicializado`);
} catch (error) {
  if (error.code !== 94 && error.codeName !== "NotYetInitialized") {
    throw error;
  }

  const result = rs.initiate(replicaSetConfig);
  if (!result.ok) {
    throw new Error(`No se pudo iniciar el Replica Set: ${tojson(result)}`);
  }
  print(`Inicialización de ${replicaSetName} solicitada`);
}

let replicaSetReady = false;

for (let attempt = 1; attempt <= 60; attempt += 1) {
  try {
    const status = rs.status();
    const primaryReady = status.members.some(
      (member) => member.name === "mongo-primary:27017" && member.stateStr === "PRIMARY"
    );
    const secondaryReady = status.members.some(
      (member) => member.name === "mongo-secondary:27017" && member.stateStr === "SECONDARY"
    );

    if (primaryReady && secondaryReady) {
      replicaSetReady = true;
      break;
    }
  } catch (error) {
    print(`Esperando elección del primario (${attempt}/60): ${error.message}`);
  }

  sleep(1000);
}

if (!replicaSetReady) {
  throw new Error(
    `El Replica Set ${replicaSetName} no quedó listo dentro del tiempo esperado`
  );
}

print(`Replica Set ${replicaSetName} listo: 1 PRIMARY y 1 SECONDARY`);
