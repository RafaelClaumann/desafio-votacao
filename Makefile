IMAGE ?= desafio-votacao
TAG   := latest

.PHONY: build list run clean

build:
	@docker build \
		--build-arg ACTIVE_PROFILE=default \
		--tag $(IMAGE):$(TAG) \
		--file Dockerfile \
		.

list:
	@docker images $(IMAGE) \
		--format 'table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}\t{{.CreatedSince}}'

run:
	@docker run --rm \
		--env ACTIVE_PROFILE=default \
		--publish 8080:8080 \
		$(IMAGE):$(TAG)

clean:
	@docker rmi $(IMAGE):$(TAG)
