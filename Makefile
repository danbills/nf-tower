config ?= compile

clean:
	./gradlew clean
	rm -rf .db
	podman rm nf-tower_db_1 || true

test:
ifndef class
	MICRONAUT_ENVIRONMENTS=mysql ./gradlew test
else
	MICRONAUT_ENVIRONMENTS=mysql ./gradlew test --tests ${class}
endif

build:
	./gradlew assemble
	./gradlew tower-backend:jibBuildTar
	podman load -i tower-backend/build/jib-image.tar
	podman build -t tower-web:latest tower-web/

run:
	podman-compose up

deps:
	./gradlew -q tower-backend:dependencies --configuration ${config}

