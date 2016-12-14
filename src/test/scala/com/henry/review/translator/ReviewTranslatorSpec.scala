package com.henry.review.translator

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.henry.review.google.GoogleApiFake
import com.henry.review.iterator.SentenceIterator
import com.henry.review.model.{Review, ReviewTranslation}
import org.scalatest.FlatSpec

import scala.collection.Iterator.fill

class ReviewTranslatorSpec extends FlatSpec {
  implicit val system = ActorSystem("TestSystem")
  implicit val dispatcher = system.dispatcher
  implicit val materializer = ActorMaterializer()

  behavior of "ReviewTranslator"

  it should "correctly translate all provided reviews and preserve their original text" in {
    val testReviews = List(
      Review(id = "1",
        text = "Right now I'm mostly just sprouting this so my cats can eat the grass. They love it. " +
          "I rotate it around with Wheatgrass and Rye too"
      ),
      Review(id = "2",
        text = "This is a confection that has been around a few centuries.  It is a light, pillowy " +
          "citrus gelatin with nuts - in this case Filberts. And it is cut into tiny squares and " +
          "then liberally coated with powdered sugar.  And it is a tiny mouthful of heaven.  Not too " +
          "chewy, and very flavorful.  I highly recommend this yummy treat.  If you are familiar with " +
          "the story of C.S. Lewis' \"The Lion, The Witch, and The Wardrobe\" - this is the treat that " +
          "seduces Edmund into selling out his Brother and Sisters to the Witch."
      ),
      Review(id = "3",
        text = "I received 4 of these in my first ever Omaha Steaks order that I received today. I wasn't " +
          "sure what to expect, but so far, I've been pleased. I tore the plastic open, put the frozen " +
          "potato on a baking sheet in the oven and put it on 350. 45 minutes later it was done and turned " +
          "out being delicious. The cheese filling was crispy on-top and the potato itself underneath was " +
          "nice and soft. No way would they come out as good if put in the microwave, so I'd say oven cooking " +
          "these is definitely the way to go. Now, you can obviously make your own stuffed baked potatoes for " +
          "far cheaper then what Omaha charges for these alone, but as long as you get a good deal on the " +
          "Omaha website, these simply make a great addition to any Omaha steaks order. As delicious are they " +
          "are though, I wouldn't order a box of JUST these off the site due to the added shipping costs. But " +
          "like I said, as long as they're part of a nicely priced packaged deal, I'll definitely take them. " +
          "Actually, I probably wouldn't order any packaged deal in the future from Omaha that didn't include " +
          "these. A big plus, at least for me, is if you check the ingredients list on these, there's no " +
          "partially hydrogenated oils, so there's no traces of trans fats, which is very rare with stuffed " +
          "baked potatoes. Also no high fructose corn syrup, which is another rarity on a food product " +
          "like this and is always a plus. If you make your own or buy them frozen from your grocery store " +
          "for example, chances are, the butter used, sour cream, etc. will be full of chemicals, trans fats " +
          "& high fructose corn syrup. These, not so much. So there is a definite degree of quality with " +
          "Omahas stuffed baked potatoes. Each potato does have 8 grams of saturated fat, which is 42% the " +
          "DRV, but that's obviously to be expected with anything that contains bacon, cheese and sour cream. " +
          "I was also expecting there to be no fiber, but it turns out there's 3grams of fiber per baked potato " +
          "half. Another plus. Overall, the stuffed baked potato was very tasty and actually relatively healthy, " +
          "being made with quality ingredients."
      ),
      Review(id = "4",
        text = "good flavor! these came securely packed... they were fresh and delicious! i love these Twizzlers!"
      ),
      Review(id = "5",
        text = "Grape gummy bears are hard to find in my area. In fact pretty much anyone I talk to about grape " +
          "gummy bears they think I'm lying. So I bought 10lbs... : ) These bears are a little bit bigger then " +
          "the other brands and have kind of sour kick, but nothing to strong. I love grape flavored candy/soda " +
          "and these are pretty good. There is another company that makes grape gummy bears that are a little bit " +
          "better in my opinion, but these are well worth it for the price. I like to use the gummy bears in home " +
          "made Popsicles with flavored sports drink. The salt in the sports drink makes for softer popsicles, " +
          "and the gummy bears are awesome frozen. They are delicious!"
      ),
      Review(id = "6",
        text = "I have a 2 year old Portuguese Water Dog who always seemed to have a sensitive stomach, and a 15 " +
          "year old Shepherd X dog who was beginning to lose weight, she slept almost all the time, and she was " +
          "getting very fussy about what she'd eat. (I believe that with a dogs sense of smell they KNOW exactly " +
          "what is in their food; however they have no choice but to eat what we feed them...just Google what is " +
          "in most commercial dog foods, and you'll see why your dog may not be thrilled to eat it up...if a dog " +
          "can detect cancer in a person, they can detect all sorts of other things that aren't supposed to be " +
          "eaten.) Anyway...I used to feed Purina lamb and rice to my dogs, but I was finding my young Porty was " +
          "having trouble with it, and my 15 yr old would eat a few bits and then leave her dish. So, I started " +
          "making their food, and came up with a wonderfully nutritious and tasty recipe. The dogs loved my homemade " +
          "dog food, and my 15 yr old began putting on weight again, but I didn't always have time to make their " +
          "food. And, homemade dog food is kind of hard on the wallet. So, I started to research all the natural " +
          "dog foods out there, compare costs etc. I arrived on Harmony Farms, and guess what...my dogs LOVE it " +
          "just as much as they love my home made dog food. My young Porty no longer has stomach troubles and she " +
          "looks great; my 15 yr old finishes her meals and looks for more. Both have shiny coats and energy. No " +
          "one can believe my old girl is 15 years old. I have told my friends about Harmony Farms dog food, and " +
          "when they've switched over, they've reported similar positive results. My brother-in-law's dog had " +
          "always been a very fussy eater, and had skin problems with a thinning coat. When they started to feed " +
          "her Harmony Farms, she became an eager eater, more happy and outgoing, and her skin and coat condition " +
          "has completely cleared up (...I'm thinking she had an allergy to whatever was in her other food, which " +
          "I think was Iams). Other friends have commented on how happy their dogs are at meal time now...and they " +
          "had all been feeding their dogs premium, top of the line dogs foods AND paying top of the line prices. " +
          "Harmony Farms products are very reasonably priced. Not the cheapest dog food on the shelf, but, in my " +
          "opinion, you are buying the best quality food on the market, so it's an incredibly GOOD DEAL! It may " +
          "also mean you make fewer trips to the veterinarian's office. I and my friends are so happy we have " +
          "found this food for our dogs. :)"
      ),
      Review(id = "7",
        text = "The Strawberry Twizzlers are my guilty pleasure - yummy. Six pounds will be around for a while " +
          "with my son and I."
      ),
      Review(id = "8",
        text = "My 1-1/2 year old basenji/jack russell mix loves this dog food. He's been noticeably healthier " +
          "and more energetic since I switched him over from the standard dog foods earlier this year. Despite " +
          "the higher cost of natural dog foods, I find that he eats significantly less of the Natural Balance " +
          "dog foods and still stays happy and full. On the normal dog foods, he'd eat up to 3 cups of dog food " +
          "a day (the recommended serving for his size), whereas he only eats about 1 cup to 1-1/2 cup of the " +
          "Natural Balance dog food a day. When you take this into account, you're actually getting more \"bang " +
          "for your buck\" with the natural dog foods since you don't have to buy as much to last just as long " +
          "as the normal dog foods... and a healthier, happier dog, to boot! Add in the fact that you can get " +
          "free, 2-day shipping with Amazon Prime... I'm sold!!"
      ),
      Review(id = "9",
        text = "I have a now 8 month old male Shih Tzu.  When I brought him home from the breeder, I kept him " +
          "on the 'Pro Plan Puppy' tiny dry kibble until the bag was almost finished.  There was about 2 cups " +
          "left in the bag and he began walking away from his bowl as soon as I put it down.  As a test, I " +
          "rolled a few morsels across the tile floor in the kitchen to see what would happen...he'd eat about " +
          "10 pieces and go bring me one of his squeaker toys to play with. (Which I was happy with, as I don't " +
          "want to 'play' with his food as a game to get him to eat).  I went and got him Blue Buffalo dry puppy " +
          "food (lamb and oatmeal formula), which had bigger size kibbles.  It seemed that he liked the bigger " +
          "sized items to chew on and the harder antioxidant bits, since he was teething and chewing on his " +
          "dentabones, venison antler, lamb lung chips for comfort.  Again when I got down to about a quarter " +
          "of food left in the bag, he would walk away from the bowl---this went on for almost 3 days.  He was " +
          "still having small bowel movements, urinating, drinking water and playing, running and continuing his " +
          "successful training exercises; called the vet and they wanted to give me wet venison formula by Science " +
          "Diet/Veterinarian's Diet after the exam, which showed nothing physically wrong.  The one vet tech said " +
          "that Shih Tzu's tend to be picky eaters.  I didn't pick up the food at their check out counter, left " +
          "and began some of my own research into the breed itself and a more natural diet.  I know from some of " +
          "our outdoor excursions that he's attracted to road kill (he'll sniff the air, and want to lunge for it " +
          "and would probably eat it if left to his own devices...but I won't allow it).  I remember being a kid in " +
          "the late 60's early 70's and all my friends who had dogs fed them 1 of 2 choices available back then: " +
          "Gravy Train or Alpo and they mixed meat or the drippings from the meat; they ate bones, etc and all " +
          "lived into their late teens.  One of my friends small mutt lived to be 24 years old!!!  I never saw " +
          "these dogs itching or digging at themselves other than the usual ears or if their was a flea problem, " +
          "since I grew up in a rural area and most often that was a common occurrance.  Well, I read up on the " +
          "more natural diets and decided to get the Sojos and ground beef and some boneless chicken thighs/breasts.  " +
          "My Weston isn't a big fan of totally raw meat, but when browned (I put it in the toaster oven and " +
          "add 1/4 tsp of 'Missing Link' to the meat portion) with the soaked Sojos Original, he eats with gusto!!!  " +
          "From his early days, I would play with and take his food away, so he's not food aggressive.  He eats " +
          "the recommended portion for his size and walks away when done.  Weston then wants to play hard, " +
          "train hard, take a nice walk and nap.  His coat is soft and easy to keep brushed and groomed.  The " +
          "droppings are nice sized and easy to clean up.  I just had him neutered 2 days ago and he ate this " +
          "Sojos mix with the same gusto (even with the e-collar to challenge him).  So I am a fan and we are " +
          "finished with the second 2.5 lb bag and I just ordered another one.  Hope this helps someone with a " +
          "picky eater; I know another reviewer mentioned their Shih Tzu not liking it, but this shows you that " +
          "dogs have certain tastes different from each other, much like us humans."
      ),
      Review(id = "10",
        text = "I am very satisfied with my Twizzler purchase.  I shared these with others and we have all enjoyed " +
          "them.  I will definitely be ordering more."
      ),
      Review(id = "11",
        text = "I'm presently on a diet and I was at my Fresh and Easy Neighborhood Grocery looking over possible " +
          "diet foods. I wanted things that were tasty, non-fat and low in calories. I came home with about a " +
          "dozen items. That's how I discovered the Tillen Farms Pickled Crispy Asparagus. Well, I've always " +
          "liked asparagus anyway and I've enjoyed several brands of pickled asparagus. This Tillen Farms brand " +
          "is really, really good! It's the best I can recall. There's an excellent flavor and a big clove of " +
          "garlic down in  the bottom of the jar which I'm looking forward to eating. I can't believe how good " +
          "this asparagus tastes and right on the front it says \"only 60 calories per jar!\" Now this is my " +
          "idea of a good diet food!"
      ),
      Review(id = "12",
        text = "I can't imagine why but it seems that grocery stores have stopped stocking this item. If you " +
          "haven't tried it you must, it is the best coating mix available and my family keeps asking me to make " +
          "chicken only with this shake and bake. It has a very different flavor than any BBQ sauce on the market, " +
          "a flavor all it's own that is really delicious and goes with any side dish. I have made stuffing, " +
          "mashed potatoes (a favorite) and almost any vegetable to go with it and everyone always has a second " +
          "helping of chicken.It's perfect for company because it is unique and tastes like something you would " +
          "cook for a special occasion. I am always afraid that they will stop making it, it really has become " +
          "a staple in our home and even the most finicky people love it, kids all seem to love it and I love it " +
          "because it is quick and easy. It's a very unique flavor, I want to say a little sweeter than most BBQ " +
          "sauces or flavorings but not really sweet , kind of like a honey BBQ but better, that can be used on " +
          "pork chops as well. There is a plastic bag enclosed to shake it in but I find that more of the mix " +
          "sticks to the sides and it is very hard to coat the chicken , a lot of wasted mix that way. I put it " +
          "on a plate a little at a time and add as needed, then I rinse the chicken pieces and kind of lightly " +
          "roll them on the plate in the coating (shaking a little out of the packet on spots that are not " +
          "coated) and then place them into a baking pan that I line with foil, it makes clean up much easier " +
          "after cooking, almost don't need to wash the pan this way.Try to get all the pieces covered especially " +
          "on the tops because the bottom will come off a little bit from the grease that cooks out of the " +
          "chicken. Any mix left in the packet I shake on pieces that don't look completely covered and then " +
          "just on top of all of them. I usually get a package of about 3 or 4 half breasts and a package of " +
          "6 thighs and one pack of mix will cover it all if you roll it on a plate. It becomes very sticky " +
          "when it gets wet so it will stick to the plate, your fingers and of course the chicken pieces. I " +
          "have also used a little bit of plum jam brushed on the chicken before coating it and thats very " +
          "good too, but honestly you don't need to add anything it is delicious the way it is. Cook it long " +
          "enough to get browned and crispy and remove it to a serving dish right away so it doesn't sit in " +
          "the grease and you will be extremely happy with the results. I would really be interested to hear " +
          "from someone who has never cooked with this before and see what they think."
      )
    )

    val expectedReviewTranslations = testReviews.map { review =>
      ReviewTranslation(
        reviewId = review.id,
        originalText = review.text,
        translatedText = fill(SentenceIterator(review.text).size)("Salut Jean, comment vas tu? ").mkString
      )
    }

    val reviewTranslator = new ReviewTranslator(new GoogleApiFake(0))

    Source(testReviews)
      .via(reviewTranslator.translateReviewFlow)
      .runWith(TestSink.probe[ReviewTranslation])
      .request(expectedReviewTranslations.size)
      .expectNextUnorderedN(expectedReviewTranslations)
  }
}