import json

def main():
    file_path = 'postman/Postman_Collection.json'
    
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
        
    prompt_folder = next((item for item in data.get('item', []) if item.get('name') == 'Prompt'), None)
    if not prompt_folder:
        print('Prompt folder not found')
        return

    # Add '프롬프트 추천' if not exists
    recommend_req_name = '프롬프트 추천'
    recommend_exists = any(i.get('name') == recommend_req_name for i in prompt_folder.get('item', []))
    if not recommend_exists:
        recommend_item = {
            "name": recommend_req_name,
            "request": {
                "method": "POST",
                "header": [
                    {"key": "Content-Type", "value": "application/json"},
                    {"key": "Authorization", "value": "Bearer {{accessToken}}"}
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n  \"request_mode\": \"ADVANCED\",\n  \"category\": \"DEVELOPMENT\",\n  \"intent\": \"GENERATE\",\n  \"raw_input\": \"REST API 설계 가이드라인을 작성해 주세요.\"\n}"
                },
                "url": {
                    "raw": "{{baseUrl}}/prompts/recommend",
                    "host": ["{{baseUrl}}"],
                    "path": ["prompts", "recommend"]
                }
            }
        }
        # Insert after 프롬프트 생성
        insert_idx = 1
        for idx, it in enumerate(prompt_folder['item']):
            if it.get('name') == '프롬프트 생성':
                insert_idx = idx + 1
                break
        prompt_folder['item'].insert(insert_idx, recommend_item)
        print("Added '프롬프트 추천'")

    # Add '확정 축 기반 프롬프트 생성' if not exists
    confirmed_req_name = '확정 축 기반 프롬프트 생성'
    confirmed_exists = any(i.get('name') == confirmed_req_name for i in prompt_folder.get('item', []))
    if not confirmed_exists:
        confirmed_item = {
            "name": confirmed_req_name,
            "request": {
                "method": "POST",
                "header": [
                    {"key": "Content-Type", "value": "application/json"},
                    {"key": "Authorization", "value": "Bearer {{accessToken}}"}
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n  \"request_mode\": \"ADVANCED\",\n  \"category\": \"DEVELOPMENT\",\n  \"intent\": \"GENERATE\",\n  \"role_type\": \"EXPERT\",\n  \"action_type\": \"EXPLAIN\",\n  \"tone\": \"PROFESSIONAL\",\n  \"style\": \"FORMATTED\",\n  \"language\": \"KOREAN\",\n  \"experience\": \"INTERMEDIATE\",\n  \"input\": \"REST API 설계 가이드라인을 작성해 주세요.\",\n  \"title\": \"API 가이드라인 프롬프트\",\n  \"description\": \"설계 시 필요한 가이드라인\",\n  \"tags\": [\"아키텍처\", \"API\"]\n}"
                },
                "url": {
                    "raw": "{{baseUrl}}/prompts/generate/confirmed",
                    "host": ["{{baseUrl}}"],
                    "path": ["prompts", "generate", "confirmed"]
                }
            }
        }
        # Insert after 프롬프트 추천
        insert_idx = 2
        for idx, it in enumerate(prompt_folder['item']):
            if it.get('name') == '프롬프트 추천':
                insert_idx = idx + 1
                break
        prompt_folder['item'].insert(insert_idx, confirmed_item)
        print("Added '확정 축 기반 프롬프트 생성'")

    # Ensure "variant": null in generate payloads
    updated_variant = 0
    def update_variant(items):
        nonlocal updated_variant
        for item in items:
            if 'item' in item:
                update_variant(item['item'])
            elif item.get('request', {}).get('url', {}).get('raw', '').endswith('/prompts/generate'):
                body = item.get('request', {}).get('body', {}).get('raw', '')
                if body:
                    try:
                        parsed = json.loads(body)
                        if 'request_type' in parsed and 'variant' not in parsed:
                            parsed['variant'] = None
                            # formatting back
                            item['request']['body']['raw'] = json.dumps(parsed, indent=2, ensure_ascii=False)
                            updated_variant += 1
                    except Exception:
                        pass
    update_variant(prompt_folder.get('item', []))
    print(f"Updated variant field in {updated_variant} places")

    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

if __name__ == '__main__':
    main()
