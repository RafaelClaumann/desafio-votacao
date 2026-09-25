.PHONY: build-jar build-image list run stop clean

# ============================
# Build
# ============================
SKIP_TESTS ?= false

build-jar:
	./mvnw -B clean package -DskipTests=$(SKIP_TESTS) --threads 2C

# ============================
# Docker
# ============================
IMAGE ?= desafio-votacao
TAG   ?= latest
PROFILE ?= default
NAME ?= app

build-image:
	@docker build \
		--build-arg ACTIVE_PROFILE=$(PROFILE) \
		--tag $(IMAGE):$(TAG) \
		--file Dockerfile \
		.

list:
	@docker images $(IMAGE) \
		--format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}\t{{.CreatedSince}}'

run:
	@docker run --detach \
		--name $(NAME) \
		--env SPRING_PROFILES_ACTIVE=$(PROFILE) \
		--publish 8080:8080 \
		$(IMAGE):$(TAG)

stop:
	@docker rm -f $(NAME) 2>/dev/null || true

clean:
	@docker rmi $(IMAGE):$(TAG)
