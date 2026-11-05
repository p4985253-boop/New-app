import os
import time
import urllib.parse
import urllib.request

def download_tts(text, lang, output_path):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    url = f"https://translate.google.com/translate_tts?ie=UTF-8&q={urllib.parse.quote(text)}&tl={lang}&client=tw-ob"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = resp.read()
                if len(data) > 500:
                    with open(output_path, "wb") as f:
                        f.write(data)
                    print(f"Downloaded: {output_path} ({len(data)} bytes)")
                    return True
        except Exception as e:
            print(f"Attempt {attempt+1} failed for {text} ({lang}): {e}")
            time.sleep(0.5)
    return False

def main():
    base_dir = "app/src/main/assets/audio"

    # English phrases & numbers
    en_words = {
        "sent": "sent",
        "rupees": "Rupees",
        "hundred": "hundred",
        "thousand": "thousand",
        "lakh": "lakh",
        "1": "one", "2": "two", "3": "three", "4": "four", "5": "five",
        "6": "six", "7": "seven", "8": "eight", "9": "nine", "10": "ten",
        "11": "eleven", "12": "twelve", "13": "thirteen", "14": "fourteen", "15": "fifteen",
        "16": "sixteen", "17": "seventeen", "18": "eighteen", "19": "nineteen", "20": "twenty",
        "30": "thirty", "40": "forty", "50": "fifty", "60": "sixty", "70": "seventy",
        "80": "eighty", "90": "ninety"
    }

    print("--- Downloading English Voice Clips ---")
    for key, text in en_words.items():
        download_tts(text, "en", os.path.join(base_dir, "en", f"{key}.mp3"))

    # Hindi phrases & numbers
    hi_words = {
        "se": "से",
        "rupaye_prapt_hue": "रुपये प्राप्त हुए",
        "sau": "सौ",
        "hazaar": "हज़ार",
        "lakh": "लाख",
        "1": "एक", "2": "दो", "3": "तीन", "4": "चार", "5": "पाँच",
        "6": "छह", "7": "सात", "8": "आठ", "9": "नौ", "10": "दस",
        "11": "ग्यारह", "12": "बारह", "13": "तेरह", "14": "चौदह", "15": "पंद्रह",
        "16": "सोलह", "17": "सत्रह", "18": "अठारह", "19": "उन्नीस", "20": "बीस",
        "30": "तीस", "40": "चालीस", "50": "पचास", "60": "साठ", "70": "सत्तर",
        "80": "अस्सी", "90": "नब्बे"
    }

    print("--- Downloading Hindi Voice Clips ---")
    for key, text in hi_words.items():
        download_tts(text, "hi", os.path.join(base_dir, "hi", f"{key}.mp3"))

    # Gujarati phrases & numbers
    gu_words = {
        "tarafthi": "તરફથી",
        "rupiya_malya": "રૂપિયા મળ્યા",
        "so": "સો",
        "hajar": "હજાર",
        "lakh": "લાખ",
        "1": "એક", "2": "બે", "3": "ત્રણ", "4": "ચાર", "5": "પાંચ",
        "6": "છ", "7": "સાત", "8": "આઠ", "9": "નવ", "10": "દસ",
        "11": "અગિયાર", "12": "બાર", "13": "તેર", "14": "ચૌદ", "15": "પંદર",
        "16": "સોળ", "17": "સત્તર", "18": "અઢાર", "19": "ઓગણીસ", "20": "વીસ",
        "30": "ત્રીસ", "40": "ચાલીસ", "50": "પચાસ", "60": "સાઈઠ", "70": "સિત્તેર",
        "80": "એંસી", "90": "નેવું"
    }

    print("--- Downloading Gujarati Voice Clips ---")
    for key, text in gu_words.items():
        download_tts(text, "gu", os.path.join(base_dir, "gu", f"{key}.mp3"))

    print("All human voice audio clips downloaded successfully!")

if __name__ == "__main__":
    main()
